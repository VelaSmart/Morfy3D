import * as THREE from "three";
import { OrbitControls } from "https://cdn.jsdelivr.net/npm/three@0.160.0/examples/jsm/controls/OrbitControls.js";

const stage = document.getElementById("morfy-3d-viewer");
const canvas = document.getElementById("morfy-3d-canvas");
if (!stage || !canvas) throw new Error("Área do visualizador MORFY não encontrada.");

const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true, powerPreference: "high-performance" });
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.12;

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(34, 1, 0.1, 100);
camera.position.set(0, 3.5, 5.8);

const controls = new OrbitControls(camera, canvas);
controls.enableDamping = true;
controls.dampingFactor = 0.055;
controls.enablePan = true;
controls.screenSpacePanning = true;
controls.minDistance = 2.8;
controls.maxDistance = 10;
controls.autoRotate = !reducedMotion;
controls.autoRotateSpeed = 1.0;
controls.target.set(0, 0, 0);

scene.add(new THREE.AmbientLight(0x9ccfff, 1.45));
const keyLight = new THREE.DirectionalLight(0xffffff, 3.2);
keyLight.position.set(-3, 6, 4);
scene.add(keyLight);
const cyanLight = new THREE.PointLight(0x13bfff, 24, 10);
cyanLight.position.set(2.4, 2.2, 2.5);
scene.add(cyanLight);
const blueLight = new THREE.PointLight(0x1459ff, 20, 10);
blueLight.position.set(-2.5, 0.5, -1.8);
scene.add(blueLight);

const modelRoot = new THREE.Group();
scene.add(modelRoot);

// Silhueta superior do símbolo oficial: um único perfil em Y,
// extrudado para baixo para formar a peça prismática em perspectiva.
function makeSymbol() {
  const shape = new THREE.Shape();
  shape.moveTo(-1.55, 0.88);
  shape.lineTo(-1.17, 1.22);
  shape.lineTo(0, 0.48);
  shape.lineTo(1.17, 1.22);
  shape.lineTo(1.55, 0.88);
  shape.lineTo(0.34, 0.08);
  shape.lineTo(0.34, -1.36);
  shape.lineTo(0, -1.58);
  shape.lineTo(-0.34, -1.36);
  shape.lineTo(-0.34, 0.08);
  shape.closePath();

  const depth = 0.62;
  const geometry = new THREE.ExtrudeGeometry(shape, {
    depth,
    bevelEnabled: true,
    bevelSegments: 4,
    steps: 1,
    bevelSize: 0.045,
    bevelThickness: 0.045,
    curveSegments: 2
  });
  // A face em Y fica na horizontal; a extrusão desce, formando
  // as paredes laterais visíveis no símbolo original.
  geometry.rotateX(Math.PI / 2);
  geometry.translate(0, -depth / 2, 0);
  geometry.computeVertexNormals();

  const sideMaterial = new THREE.MeshStandardMaterial({
    color: 0x0750a8,
    metalness: 0.58,
    roughness: 0.25
  });
  const topMaterial = new THREE.MeshStandardMaterial({
    color: 0x35c8f3,
    metalness: 0.34,
    roughness: 0.23
  });
  const solid = new THREE.Mesh(geometry, [sideMaterial, topMaterial]);
  modelRoot.add(solid);

  // Reforço azul nas paredes da frente para preservar a leitura
  // de volume e a divisão cromática da marca oficial.
  const frontMaterial = new THREE.MeshStandardMaterial({
    color: 0x0750a8,
    metalness: 0.52,
    roughness: 0.27
  });
  const frontFace = new THREE.Mesh(
    new THREE.BoxGeometry(0.68, 0.025, 1.46),
    frontMaterial
  );
  frontFace.position.set(0, -0.76, 0);
  frontFace.visible = false;
  modelRoot.add(frontFace);

  modelRoot.rotation.set(0, -0.16, 0);
  const bounds = new THREE.Box3().setFromObject(modelRoot);
  modelRoot.position.sub(bounds.getCenter(new THREE.Vector3()));
}

makeSymbol();
stage.classList.add("is-ready");

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

stage.querySelectorAll("[data-viewer-action]").forEach(button => {
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
    } else if (action === "reset") {
      camera.position.set(0, 3.5, 5.8);
      controls.target.set(0, 0, 0);
      modelRoot.rotation.set(0, -0.16, 0);
      controls.autoRotate = !reducedMotion;
      const toggle = stage.querySelector('[data-viewer-action="rotate"]');
      toggle?.setAttribute("aria-pressed", String(controls.autoRotate));
      controls.update();
    }
  });
});

function animate() {
  requestAnimationFrame(animate);
  controls.update();
  renderer.render(scene, camera);
}
animate();

window.addEventListener("pagehide", () => {
  resizeObserver.disconnect();
  controls.dispose();
  renderer.dispose();
}, { once: true });
