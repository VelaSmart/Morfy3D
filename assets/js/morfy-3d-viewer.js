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
const camera = new THREE.PerspectiveCamera(37, 1, 0.1, 100);
camera.position.set(0, 3.7, 6.3);

const controls = new OrbitControls(camera, canvas);
controls.enableDamping = true;
controls.dampingFactor = 0.055;
controls.enablePan = true;
controls.screenSpacePanning = true;
controls.minDistance = 3.4;
controls.maxDistance = 11;
controls.autoRotate = !reducedMotion;
controls.autoRotateSpeed = 0.42;
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

// Perfil do símbolo oficial: braços simétricos, haste central e cantos
// levemente chanfrados para preservar a leitura da peça em qualquer ângulo.
function makeSymbol() {
  const shape = new THREE.Shape();
  shape.moveTo(-1.55, 1.18);
  shape.lineTo(-1.55, 0.91);
  shape.lineTo(0, 0.08);
  shape.lineTo(1.55, 0.91);
  shape.lineTo(1.55, 1.18);
  shape.lineTo(0.34, 0.51);
  shape.lineTo(0.34, -1.19);
  shape.lineTo(0, -1.42);
  shape.lineTo(-0.34, -1.19);
  shape.lineTo(-0.34, 0.51);
  shape.closePath();

  const depth = 0.58;
  let geometry = new THREE.ExtrudeGeometry(shape, {
    depth,
    bevelEnabled: true,
    bevelSegments: 2,
    steps: 1,
    bevelSize: 0.035,
    bevelThickness: 0.035,
    curveSegments: 2
  });

  // A face do Y fica no plano horizontal; a extrusão desce para criar as
  // laterais. Separar as faces por orientação mantém o topo ciano visível
  // e aplica as duas cores oficiais aos lados esquerdo e direito.
  geometry.rotateX(Math.PI / 2);
  geometry.translate(0, -depth / 2, 0);
  if (geometry.index) geometry = geometry.toNonIndexed();
  geometry.computeVertexNormals();

  const positions = geometry.getAttribute("position");
  geometry.clearGroups();
  let groupMaterial = -1;
  let groupStart = 0;

  for (let vertex = 0; vertex < positions.count; vertex += 3) {
    const ax = positions.getX(vertex);
    const ay = positions.getY(vertex);
    const az = positions.getZ(vertex);
    const bx = positions.getX(vertex + 1);
    const by = positions.getY(vertex + 1);
    const bz = positions.getZ(vertex + 1);
    const cx = positions.getX(vertex + 2);
    const cy = positions.getY(vertex + 2);
    const cz = positions.getZ(vertex + 2);

    const abx = bx - ax;
    const aby = by - ay;
    const abz = bz - az;
    const acx = cx - ax;
    const acy = cy - ay;
    const acz = cz - az;
    const normalY = abz * acx - abx * acz;
    const normalX = aby * acz - abz * acy;
    const normalZ = abx * acy - aby * acx;
    const normalLength = Math.hypot(normalX, normalY, normalZ);
    const isTopFace = normalLength > 0 && Math.abs(normalY) / normalLength > 0.85;
    const materialIndex = isTopFace
      ? 2
      : (ax + bx + cx) / 3 < 0 ? 0 : 1;

    if (materialIndex !== groupMaterial) {
      if (groupMaterial !== -1) geometry.addGroup(groupStart, vertex - groupStart, groupMaterial);
      groupMaterial = materialIndex;
      groupStart = vertex;
    }
  }
  if (groupMaterial !== -1) {
    geometry.addGroup(groupStart, positions.count - groupStart, groupMaterial);
  }

  const leftMaterial = new THREE.MeshStandardMaterial({
    color: 0x0b91bb,
    metalness: 0.32,
    roughness: 0.3
  });
  const rightMaterial = new THREE.MeshStandardMaterial({
    color: 0x0b45bd,
    metalness: 0.32,
    roughness: 0.3
  });
  const topMaterial = new THREE.MeshStandardMaterial({
    color: 0x52d6f4,
    metalness: 0.18,
    roughness: 0.26
  });
  const solid = new THREE.Mesh(geometry, [leftMaterial, rightMaterial, topMaterial]);
  modelRoot.add(solid);

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
      camera.position.set(0, 3.7, 6.3);
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
