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

scene.add(new THREE.HemisphereLight(0xc9eaff, 0x071326, 1.8));
const key = new THREE.DirectionalLight(0xffffff, 3.0);
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

const loader = new GLTFLoader();
const MODEL_URL = "./assets/models/morfy-logo.glb";
const status = stage.querySelector("[data-viewer-status]");

function setStatus(message) {
  if (status) status.textContent = message;
}

function frameModel(object) {
  const bounds = new THREE.Box3().setFromObject(object);
  const size = bounds.getSize(new THREE.Vector3());
  const center = bounds.getCenter(new THREE.Vector3());
  object.position.sub(center);

  const maxDimension = Math.max(size.x, size.y, size.z) || 1;
  const scale = 2.15 / maxDimension;
  object.scale.setScalar(scale);

  const fitted = new THREE.Box3().setFromObject(object);
  const fittedSize = fitted.getSize(new THREE.Vector3());
  const distance = Math.max(fittedSize.x, fittedSize.y, fittedSize.z) * 2.25;
  camera.position.set(distance * 0.12, distance * 0.18, distance);
  camera.near = Math.max(0.001, distance / 100);
  camera.far = distance * 20;
  camera.updateProjectionMatrix();
  controls.target.set(0, 0, 0);
  controls.minDistance = distance * 0.65;
  controls.maxDistance = distance * 4.5;
  controls.update();
}

loader.load(
  MODEL_URL,
  (gltf) => {
    const model = gltf.scene;
    model.traverse((node) => {
      if (!node.isMesh) return;
      node.castShadow = true;
      node.receiveShadow = true;
      if (node.material) {
        const materials = Array.isArray(node.material) ? node.material : [node.material];
        for (const material of materials) {
          if ("roughness" in material && material.roughness > 0.42) material.roughness = 0.42;
          if ("metalness" in material && material.metalness < 0.12) material.metalness = 0.12;
          material.needsUpdate = true;
        }
      }
    });
    modelRoot.add(model);
    frameModel(modelRoot);
    stage.classList.add("is-ready");
    setStatus("");
  },
  undefined,
  (error) => {
    console.error("Falha ao carregar o modelo 3D original da MORFY:", error);
    setStatus("Modelo 3D original não encontrado em assets/models/morfy-logo.glb.");
    stage.classList.add("is-error");
  }
);

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
