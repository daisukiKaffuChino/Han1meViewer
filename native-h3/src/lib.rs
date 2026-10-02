use std::net::{IpAddr, Ipv4Addr, Ipv6Addr, SocketAddr};
use std::time::{Duration, Instant};

use quiche::h3::{Config as H3Config, Connection as H3Connection, Header, NameValue};

fn json_escape(value: &str) -> String {
    let mut output = String::with_capacity(value.len() + 8);
    for character in value.chars() {
        match character {
            '"' => output.push_str("\\\""),
            '\\' => output.push_str("\\\\"),
            '\n' => output.push_str("\\n"),
            '\r' => output.push_str("\\r"),
            '\t' => output.push_str("\\t"),
            c if (c as u32) < 0x20 => output.push_str(&format!("\\u{:04x}", c as u32)),
            c => output.push(c),
        }
    }
    output
}

fn result_json(ok: bool, status: u16, body_len: usize, saved_to: &str, error: &str) -> String {
    format!(
        "{{\"ok\":{},\"status\":{},\"body_len\":{},\"saved_to\":\"{}\",\"error\":\"{}\"}}",
        ok,
        status,
        body_len,
        json_escape(saved_to),
        json_escape(error),
    )
}

fn h3_fetch(
    host: &str,
    peer_ip: &str,
    ech: Option<&[u8]>,
    path: &str,
    referer: Option<&str>,
    ca_path: &str,
    output_file: &str,
    user_agent: &str,
) -> String {
    let peer: SocketAddr = match format!("{}:443", peer_ip).parse() {
        Ok(value) => value,
        Err(error) => return result_json(false, 0, 0, "", &format!("Invalid peer IP: {error}")),
    };
    let local = if peer.is_ipv4() {
        SocketAddr::new(IpAddr::V4(Ipv4Addr::UNSPECIFIED), 0)
    } else {
        SocketAddr::new(IpAddr::V6(Ipv6Addr::UNSPECIFIED), 0)
    };

    let mut config = match quiche::Config::new(quiche::PROTOCOL_VERSION) {
        Ok(value) => value,
        Err(error) => return result_json(false, 0, 0, "", &format!("QUIC config failed: {error:?}")),
    };
    config.verify_peer(true);
    if let Err(error) = config.load_verify_locations_from_file(ca_path) {
        return result_json(false, 0, 0, "", &format!("CA load failed: {error:?}"));
    }
    if let Err(error) = config.set_application_protos(&[b"h3"]) {
        return result_json(false, 0, 0, "", &format!("ALPN setup failed: {error:?}"));
    }
    config.set_max_idle_timeout(30_000);
    config.set_max_recv_udp_payload_size(1350);
    config.set_initial_max_data(10_000_000);
    config.set_initial_max_stream_data_bidi_local(1_000_000);
    config.set_initial_max_stream_data_bidi_remote(1_000_000);
    config.set_initial_max_stream_data_uni(1_000_000);
    config.set_initial_max_streams_bidi(100);
    config.set_initial_max_streams_uni(100);
    config.set_disable_active_migration(true);
    config.set_cc_algorithm(quiche::CongestionControlAlgorithm::CUBIC);
    if let Some(value) = ech {
        config.set_ech_config_list(value);
    }

    let socket = match std::net::UdpSocket::bind(local) {
        Ok(value) => value,
        Err(error) => return result_json(false, 0, 0, "", &format!("UDP bind failed: {error}")),
    };
    if let Err(error) = socket.connect(peer) {
        return result_json(false, 0, 0, "", &format!("UDP connect failed: {error}"));
    }
    let local_addr = match socket.local_addr() {
        Ok(value) => value,
        Err(error) => return result_json(false, 0, 0, "", &format!("UDP address failed: {error}")),
    };

    let scid_bytes: [u8; 16] = rand::random();
    let scid = quiche::ConnectionId::from_vec(scid_bytes.to_vec());
    let mut connection = match quiche::connect(Some(host), &scid, local_addr, peer, &mut config) {
        Ok(value) => value,
        Err(error) => return result_json(false, 0, 0, "", &format!("QUIC connect failed: {error:?}")),
    };

    let mut buffer = [0u8; 65535];
    let handshake_started = Instant::now();
    while !connection.is_established() {
        while let Ok((length, info)) = connection.send(&mut buffer) {
            let _ = socket.send_to(&buffer[..length], info.to);
        }
        if let Some(error) = connection.peer_error() {
            return result_json(false, 0, 0, "", &format!("Peer error: {error:?}"));
        }
        if connection.is_closed() {
            return result_json(false, 0, 0, "", "Connection closed during handshake");
        }
        if handshake_started.elapsed() >= Duration::from_secs(8) {
            return result_json(false, 0, 0, "", "QUIC handshake timed out");
        }

        let remaining = Duration::from_secs(8).saturating_sub(handshake_started.elapsed());
        let _ = socket.set_read_timeout(Some(remaining.min(Duration::from_millis(500))));
        match socket.recv_from(&mut buffer) {
            Ok((length, from)) => {
                let info = quiche::RecvInfo {
                    from,
                    to: local_addr,
                };
                if let Err(error) = connection.recv(&mut buffer[..length], info) {
                    return result_json(false, 0, 0, "", &format!("QUIC receive failed: {error:?}"));
                }
            }
            Err(error)
                if error.kind() == std::io::ErrorKind::WouldBlock
                    || error.kind() == std::io::ErrorKind::TimedOut => {}
            Err(error) => {
                return result_json(false, 0, 0, "", &format!("UDP receive failed: {error}"));
            }
        }
        connection.on_timeout();
    }

    let mut h3_config = match H3Config::new() {
        Ok(value) => value,
        Err(error) => return result_json(false, 0, 0, "", &format!("H3 config failed: {error:?}")),
    };
    let mut h3 = match H3Connection::with_transport(&mut connection, &mut h3_config) {
        Ok(value) => value,
        Err(error) => return result_json(false, 0, 0, "", &format!("H3 connection failed: {error:?}")),
    };

    let mut headers = vec![
        Header::new(b":method", b"GET"),
        Header::new(b":scheme", b"https"),
        Header::new(b":authority", host.as_bytes()),
        Header::new(b":path", path.as_bytes()),
        Header::new(b"user-agent", user_agent.as_bytes()),
        Header::new(b"accept", b"image/avif,image/webp,*/*"),
    ];
    if let Some(value) = referer {
        headers.push(Header::new(b"referer", value.as_bytes()));
    }

    let request_started = Instant::now();
    if let Err(error) = h3.send_request(&mut connection, &headers, true) {
        return result_json(false, 0, 0, "", &format!("H3 request failed: {error:?}"));
    }

    let mut status = 0u16;
    let mut body = Vec::new();
    let mut finished = false;
    while !finished {
        while let Ok((length, info)) = connection.send(&mut buffer) {
            let _ = socket.send_to(&buffer[..length], info.to);
        }

        loop {
            match h3.poll(&mut connection) {
                Ok((stream_id, quiche::h3::Event::Headers { list, .. })) => {
                    for header in list.iter() {
                        let name = String::from_utf8_lossy(NameValue::name(header)).to_lowercase();
                        if name == ":status" {
                            status = String::from_utf8_lossy(NameValue::value(header))
                                .parse()
                                .unwrap_or(0);
                        }
                    }
                    let _ = stream_id;
                }
                Ok((stream_id, quiche::h3::Event::Data)) => {
                    let mut chunk = vec![0u8; 65535];
                    match h3.recv_body(&mut connection, stream_id, &mut chunk) {
                        Ok(length) => body.extend_from_slice(&chunk[..length]),
                        Err(quiche::h3::Error::Done) => {}
                        Err(error) => {
                            return result_json(false, status, body.len(), "", &format!("Body read failed: {error:?}"));
                        }
                    }
                }
                Ok((_, quiche::h3::Event::Finished)) => {
                    finished = true;
                    break;
                }
                Ok((_, quiche::h3::Event::Reset(error))) => {
                    return result_json(false, status, body.len(), "", &format!("Stream reset: {error}"));
                }
                Ok((_, quiche::h3::Event::PriorityUpdate))
                | Ok((_, quiche::h3::Event::GoAway)) => {}
                Err(quiche::h3::Error::Done) => break,
                Err(error) => {
                    return result_json(false, status, body.len(), "", &format!("H3 poll failed: {error:?}"));
                }
            }
        }

        if finished {
            break;
        }
        if request_started.elapsed() >= Duration::from_secs(25) {
            return result_json(false, status, body.len(), "", "H3 request timed out");
        }

        let remaining = Duration::from_secs(25).saturating_sub(request_started.elapsed());
        let _ = socket.set_read_timeout(Some(remaining.min(Duration::from_millis(500))));
        match socket.recv_from(&mut buffer) {
            Ok((length, from)) => {
                let info = quiche::RecvInfo {
                    from,
                    to: local_addr,
                };
                let _ = connection.recv(&mut buffer[..length], info);
            }
            Err(_) => {}
        }
        connection.on_timeout();
    }

    if status != 200 {
        return result_json(false, status, body.len(), "", &format!("HTTP status {status}"));
    }
    if body.is_empty() {
        return result_json(false, status, 0, "", "Empty H3 response");
    }
    if let Err(error) = std::fs::write(output_file, &body) {
        return result_json(false, status, body.len(), "", &format!("Write failed: {error}"));
    }
    result_json(true, status, body.len(), output_file, "")
}

fn base64_decode(value: &str) -> Result<Vec<u8>, String> {
    const TABLE: &[u8; 64] =
        b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let mut lookup = [255u8; 256];
    for (index, character) in TABLE.iter().enumerate() {
        lookup[*character as usize] = index as u8;
    }

    let mut output = Vec::new();
    let mut accumulator = 0u32;
    let mut bits = 0u32;
    for character in value.bytes() {
        if character == b'=' || character.is_ascii_whitespace() {
            continue;
        }
        let decoded = lookup[character as usize];
        if decoded == 255 {
            return Err(format!("Invalid base64 character: {character}"));
        }
        accumulator = (accumulator << 6) | decoded as u32;
        bits += 6;
        if bits >= 8 {
            bits -= 8;
            output.push(((accumulator >> bits) & 0xFF) as u8);
        }
    }
    Ok(output)
}

mod android_jni {
    use super::*;
    use jni::objects::{JClass, JString};
    use jni::sys::jstring;
    use jni::JNIEnv;

    fn jstring(env: &mut JNIEnv, value: JString) -> String {
        env.get_string(&value)
            .map(|string| string.to_string_lossy().into_owned())
            .unwrap_or_default()
    }

    #[no_mangle]
    pub extern "system" fn Java_io_github_daisukikaffuchino_han1meviewer_logic_network_ech_HyEchH3_h3Fetch(
        mut env: JNIEnv,
        _class: JClass,
        host: JString,
        peer_ip: JString,
        ech_base64: JString,
        path: JString,
        referer: JString,
        ca_path: JString,
        output_file: JString,
        user_agent: JString,
    ) -> jstring {
        let host = jstring(&mut env, host);
        let peer_ip = jstring(&mut env, peer_ip);
        let ech_base64 = jstring(&mut env, ech_base64);
        let path = jstring(&mut env, path);
        let referer = jstring(&mut env, referer);
        let ca_path = jstring(&mut env, ca_path);
        let output_file = jstring(&mut env, output_file);
        let user_agent = jstring(&mut env, user_agent);

        let ech = if ech_base64.is_empty() {
            None
        } else {
            match base64_decode(&ech_base64) {
                Ok(value) => Some(value),
                Err(error) => {
                    let response = result_json(false, 0, 0, "", &error);
                    return env.new_string(response).unwrap().into_raw();
                }
            }
        };

        let response = h3_fetch(
            &host,
            &peer_ip,
            ech.as_deref(),
            if path.is_empty() { "/" } else { &path },
            if referer.is_empty() { None } else { Some(&referer) },
            if ca_path.is_empty() { "/system/etc/security/cacerts" } else { &ca_path },
            &output_file,
            &user_agent,
        );
        env.new_string(response).unwrap().into_raw()
    }
}
