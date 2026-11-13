use serde::Serialize;
use std::{
    io::{BufRead, BufReader},
    process::{Child, Command, Stdio},
    sync::{Arc, Mutex},
    thread,
};
use tauri::{Manager, RunEvent};

#[derive(Serialize)]
struct PortInfo { name: String, description: String }

#[derive(Default)]
struct BridgeState {
    child: Mutex<Option<Child>>,
    pin: Arc<Mutex<Option<String>>>,
    error: Arc<Mutex<Option<String>>>,
}

#[derive(Serialize)]
struct BridgeStatus {
    running: bool,
    pin: Option<String>,
    message: String,
}

#[tauri::command]
fn bridge_status(state: tauri::State<'_, BridgeState>) -> BridgeStatus {
    let mut child = state.child.lock().expect("bridge child mutex poisoned");
    let running = match child.as_mut() {
        Some(process) => process.try_wait().map(|status| status.is_none()).unwrap_or(false),
        None => false,
    };
    let pin = state.pin.lock().expect("bridge pin mutex poisoned").clone();
    let error = state.error.lock().expect("bridge error mutex poisoned").clone();
    BridgeStatus {
        running,
        pin,
        message: if running { "Bridge integrada ativa".into() }
            else { error.unwrap_or_else(|| "Serviço de conexão não iniciado".into()) },
    }
}

#[tauri::command]
fn list_serial_ports() -> Result<Vec<PortInfo>, String> {
    let ports = serialport::available_ports().map_err(|e| e.to_string())?;
    Ok(ports.into_iter().map(|port| {
        let description = match port.port_type {
            serialport::SerialPortType::UsbPort(info) => format!("USB VID:{:04X} PID:{:04X}", info.vid, info.pid),
            serialport::SerialPortType::BluetoothPort => "Bluetooth".to_string(),
            serialport::SerialPortType::PciPort => "PCI".to_string(),
            serialport::SerialPortType::Unknown => "Serial".to_string(),
        };
        PortInfo { name: port.port_name, description }
    }).collect())
}

#[tauri::command]
fn test_serial_connection(port_name: String, baud_rate: u32) -> Result<String, String> {
    if baud_rate == 0 || baud_rate > 1_000_000 { return Err("Baud rate inválido".to_string()); }
    let port = serialport::new(&port_name, baud_rate)
        .timeout(std::time::Duration::from_millis(300))
        .open().map_err(|e| format!("Não foi possível abrir {}: {}", port_name, e))?;
    drop(port);
    Ok(format!("Acesso à porta {} validado", port_name))
}

#[tauri::command]
fn send_serial_line(line: String) -> Result<(), String> {
    if line.len() > 512 || line.contains('\n') || line.contains('\r') {
        return Err("Mensagem inválida ou excede o limite permitido".to_string());
    }
    Err("Envio serial ainda não habilitado no scaffold".to_string())
}

fn start_bridge(app: &tauri::App) {
    let state = BridgeState::default();
    let pin_slot = state.pin.clone();
    let error_slot = state.error.clone();
    let mut candidates = Vec::new();
    candidates.push(std::path::PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("bridge-publish").join("DeckraBridge.Service.exe"));
    if let Ok(resource_dir) = app.path().resource_dir() {
        candidates.push(resource_dir.join("bridge").join("DeckraBridge.Service.exe"));
    }
    if let Ok(exe) = std::env::current_exe() {
        if let Some(parent) = exe.parent() {
            candidates.push(parent.join("bridge-publish").join("DeckraBridge.Service.exe"));
            candidates.push(parent.join("resources").join("bridge").join("DeckraBridge.Service.exe"));
        }
    }
    let executable = candidates.into_iter().find(|path| path.is_file());
    if let Some(path) = executable {
        let mut command = Command::new(path);
        command.stdin(Stdio::null()).stdout(Stdio::piped()).stderr(Stdio::null());
        #[cfg(windows)]
        {
            use std::os::windows::process::CommandExt;
            command.creation_flags(0x08000000);
        }
        match command.spawn() {
            Ok(mut child) => {
                if let Some(stdout) = child.stdout.take() {
                    thread::spawn(move || {
                        for line in BufReader::new(stdout).lines().flatten() {
                            if let Some(message) = line.strip_prefix("DECKRA_ERROR=") {
                                *error_slot.lock().expect("error mutex poisoned") = Some(message.to_string());
                            }
                            if let Some(value) = line.strip_prefix("DECKRA_PIN=") {
                                if value.len() == 6 && value.chars().all(|c| c.is_ascii_digit()) {
                                    *pin_slot.lock().expect("pin mutex poisoned") = Some(value.to_string());
                                }
                            }
                        }
                    });
                }
                *state.child.lock().expect("bridge child mutex poisoned") = Some(child);
            }
            Err(error) => *error_slot.lock().expect("bridge error mutex poisoned") =
                Some(format!("Não foi possível iniciar o serviço integrado: {error}")),
        }
    } else {
        *error_slot.lock().expect("bridge error mutex poisoned") =
            Some("Serviço integrado ausente. Compile o pacote Windows com .NET 8.".into());
    }
    app.manage(state);
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let app = tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![list_serial_ports, test_serial_connection, send_serial_line, bridge_status])
        .setup(|app| {
            start_bridge(app);
            Ok(())
        })
        .build(tauri::generate_context!())
        .expect("erro ao iniciar DECKRA Control Center");

    app.run(|handle, event| {
        if matches!(event, RunEvent::Exit) {
            if let Some(state) = handle.try_state::<BridgeState>() {
                if let Ok(mut child) = state.child.lock() {
                    if let Some(process) = child.as_mut() {
                        let _ = process.kill();
                        let _ = process.wait();
                    }
                }
            }
        }
    });
}
