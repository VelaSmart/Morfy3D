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
renderer.toneMappingExposure = 1.15;

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(34, 1, 0.1, 100);
camera.position.set(0, 0, 6.4);

const controls = new OrbitControls(camera, canvas);
controls.enableDamping = true;
controls.dampingFactor = 0.055;
controls.enablePan = true;
controls.screenSpacePanning = true;
controls.minDistance = 2.8;
controls.maxDistance = 10;
controls.autoRotate = !reducedMotion;
controls.autoRotateSpeed = 1.1;
controls.target.set(0, 0, 0);

scene.add(new THREE.AmbientLight(0x9ccfff, 1.5));
const keyLight = new THREE.DirectionalLight(0xffffff, 3.4);
keyLight.position.set(-3, 4, 6);
scene.add(keyLight);
const cyanLight = new THREE.PointLight(0x13bfff, 34, 10);
cyanLight.position.set(2.4, 0.4, 2.5);
scene.add(cyanLight);
const blueLight = new THREE.PointLight(0x1459ff, 28, 10);
blueLight.position.set(-2.5, -1.5, 1.8);
scene.add(blueLight);

const modelRoot = new THREE.Group();
scene.add(modelRoot);

// A marca é construída como três braços prismáticos que se encontram no
// mesmo vértice: a peça aberta de um cubo, sem fechar as faces ausentes.
function createRoundedBeam(length, width, depth, color) {
  const shape = new THREE.Shape();
  const half = width / 2;
  shape.moveTo(-half, 0);
  shape.lineTo(half, 0);
  shape.lineTo(half, length);
  shape.lineTo(-half, length);
  shape.closePath();

  const geometry = new THREE.ExtrudeGeometry(shape, {
    depth,
    bevelEnabled: true,
    bevelSegments: 3,
    steps: 1,
    bevelSize: 0.055,
    bevelThickness: 0.055,
    curveSegments: 2
  });
  geometry.translate(0, 0, -depth / 2);
  geometry.computeVertexNormals();

  const material = new THREE.MeshStandardMaterial({
    color,
    metalness: 0.58,
    roughness: 0.24
  });
  return new THREE.Mesh(geometry, material);
}

function makeModel() {
  const beamLength = 1.78;
  const beamWidth = 0.68;
  const beamDepth = 0.68;
  const arms = [
    { angle: 150, color: 0x21c7f4 },
    { angle: 30, color: 0x0878e8 },
    { angle: -90, color: 0x087fc0 }
  ];

  arms.forEach(({ angle, color }) => {
    const radians = THREE.MathUtils.degToRad(angle);
    const beam = createRoundedBeam(beamLength, beamWidth, beamDepth, color);
    // O eixo local +Y aponta para fora do vértice central.
    beam.rotation.z = radians - Math.PI / 2;
    const centerDistance = beamLength / 2 - beamWidth * 0.24;
    beam.position.set(
      Math.cos(radians) * centerDistance,
      Math.sin(radians) * centerDistance,
      0
    );
    modelRoot.add(beam);
  });

  // Pequeno núcleo arredondado une visualmente os três braços e mantém
  // a leitura de uma única peça estrutural.
  const core = new THREE.Mesh(
    new THREE.BoxGeometry(beamWidth * 0.94, beamWidth * 0.94, beamDepth * 0.94),
    new THREE.MeshStandardMaterial({ color: 0x078ed2, metalness: 0.62, roughness: 0.22 })
  );
  modelRoot.add(core);

  modelRoot.rotation.set(-0.16, -0.28, 0);
  const bounds = new THREE.Box3().setFromObject(modelRoot);
  modelRoot.position.sub(bounds.getCenter(new THREE.Vector3()));
}

makeModel();
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
      camera.position.set(0, 0, 6.4);
      controls.target.set(0, 0, 0);
      modelRoot.rotation.set(-0.16, -0.28, 0);
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
