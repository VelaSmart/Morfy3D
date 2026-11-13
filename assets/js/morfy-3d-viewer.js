import * as THREE from "three";
import { OrbitControls } from "https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/controls/OrbitControls.js";
import { GLTFLoader } from "https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/loaders/GLTFLoader.js";

const stage = document.getElementById("morfy-3d-viewer");
const canvas = document.getElementById("morfy-3d-canvas");
if (!stage || !canvas) throw new Error("Área do visualizador MORFY não encontrada.");

const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true, powerPreference: "high-performance" });
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.08;

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(35, 1, 0.01, 100);
camera.position.set(0, 1.15, 3.6);
const controls = new OrbitControls(camera, canvas);
controls.enableDamping = true;
controls.dampingFactor = 0.055;
controls.enablePan = false;
controls.minDistance = 1.1;
controls.maxDistance = 8;
controls.autoRotate = !reducedMotion;
controls.autoRotateSpeed = 0.35;
// Limita o giro horizontal a um arco total de 160 graus (80° para cada lado).
controls.minAzimuthAngle = THREE.MathUtils.degToRad(-80);
controls.maxAzimuthAngle = THREE.MathUtils.degToRad(80);

scene.add(new THREE.HemisphereLight(0xc9eaff, 0x071326, 1.8));
const key = new THREE.DirectionalLight(0xffffff, 3);
key.position.set(-3, 5, 4);
scene.add(key);
const cyan = new THREE.PointLight(0x16c8ff, 18, 8);
cyan.position.set(2, 1.5, 2.5);
scene.add(cyan);
const blue = new THREE.PointLight(0x155bff, 13, 8);
blue.position.set(-2, -1, -2);
scene.add(blue);

const modelRoot = new THREE.Group();
scene.add(modelRoot);

// O visualizador carrega a malha original. Não há mais uma silhueta Y
// aproximada construída por código: a geometria e os materiais vêm do GLB.
const status = document.createElement("p");
status.className = "viewer-model-status";
status.setAttribute("role", "status");
status.style.cssText = "position:absolute;z-index:3;left:50%;top:50%;transform:translate(-50%,-50%);max-width:min(82%,420px);padding:12px 16px;border:1px solid rgba(91,190,255,.35);border-radius:12px;background:rgba(3,12,29,.82);color:#c9eaff;font:500 13px/1.5 system-ui,sans-serif;text-align:center;pointer-events:none";
stage.append(status);

function frameModel(object) {
  object.updateWorldMatrix(true, true);
  const bounds = new THREE.Box3().setFromObject(object);
  const size = bounds.getSize(new THREE.Vector3());
  const center = bounds.getCenter(new THREE.Vector3());
  // Centraliza a malha pelo centro geométrico real, não pela origem do arquivo GLB.
  object.position.sub(center);
  object.updateWorldMatrix(true, true);
  const maxDimension = Math.max(size.x, size.y, size.z) || 1;
  object.scale.setScalar(2.15 / maxDimension);
  const distance = 2.15 * 2.25;
  camera.position.set(distance * 0.12, distance * 0.18, distance);
  camera.near = 0.01;
  camera.far = 100;
  camera.updateProjectionMatrix();
  controls.target.set(0, 0, 0);
  controls.minAzimuthAngle = THREE.MathUtils.degToRad(-80);
  controls.maxAzimuthAngle = THREE.MathUtils.degToRad(80);
  controls.minDistance = 1.1;
  controls.maxDistance = 8;
  controls.update();
}

async function loadOriginalModel() {
  try {
    const response = await fetch("./assets/models/morfy-logo.glb.gz");
    if (!response.ok) throw new Error(`HTTP ${response.status} ao buscar o modelo compactado`);
    if (!("DecompressionStream" in window)) throw new Error("Este navegador não oferece descompactação gzip nativa.");
    const compressed = await response.arrayBuffer();
    const stream = new Blob([compressed]).stream().pipeThrough(new DecompressionStream("gzip"));
    const glbBuffer = await new Response(stream).arrayBuffer();
    new GLTFLoader().parse(glbBuffer, "./assets/models/", (gltf) => {
      const model = gltf.scene;
      model.traverse((node) => {
        if (!node.isMesh) return;
        node.castShadow = true;
        node.receiveShadow = true;
        const materials = Array.isArray(node.material) ? node.material : [node.material];
        for (const material of materials) {
          if ("roughness" in material) material.roughness = Math.min(material.roughness, 0.42);
          material.needsUpdate = true;
        }
      });
      modelRoot.add(model);
      frameModel(modelRoot);
      status.remove();
      stage.classList.add("is-ready");
    }, (error) => {
      console.error("Falha ao interpretar o GLB original da MORFY:", error);
      status.textContent = "Não foi possível interpretar o modelo 3D original.";
      stage.classList.add("is-error");
    });
  } catch (error) {
    console.error("Não foi possível carregar o modelo 3D original da MORFY:", error);
    status.textContent = "Não foi possível carregar o modelo 3D compactado. Confira se morfy-logo.glb.gz está em assets/models.";
    stage.classList.add("is-error");
  }
}
loadOriginalModel();

function resize() {
  const width = Math.max(1, stage.clientWidth);
  const height = Math.max(1, stage.clientHeight);
  renderer.setSize(width, height, false);
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
}
const resizeObserver = new ResizeObserver(resize);
resizeObserver.observe(stage);
resize();

stage.querySelectorAll("[data-viewer-action]").forEach((button) => {
  button.addEventListener("click", () => {
    const action = button.dataset.viewerAction;
    if (action === "rotate") {
      controls.autoRotate = !controls.autoRotate;
      button.setAttribute("aria-pressed", String(controls.autoRotate));
    } else if (action === "zoom-in") {
      camera.position.multiplyScalar(0.82);
      controls.update();
    } else if (action === "zoom-out") {
      camera.position.multiplyScalar(1.22);
      controls.update();
    } else if (action === "reset" && modelRoot.children.length) {
      frameModel(modelRoot);
      controls.autoRotate = !reducedMotion;
      stage.querySelector('[data-viewer-action="rotate"]')?.setAttribute("aria-pressed", String(controls.autoRotate));
    }
  });
});

let animationFrame = 0;
function animate() {
  animationFrame = requestAnimationFrame(animate);
  controls.update();
  renderer.render(scene, camera);
}
animate();

window.addEventListener("pagehide", () => {
  cancelAnimationFrame(animationFrame);
  resizeObserver.disconnect();
  controls.dispose();
  renderer.dispose();
  modelRoot.traverse((node) => {
    if (!node.isMesh) return;
    node.geometry?.dispose();
    for (const material of (Array.isArray(node.material) ? node.material : [node.material])) material?.dispose();
  });
}, { once: true });
