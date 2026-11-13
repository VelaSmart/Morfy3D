import * as THREE from "https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js";
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
camera.position.set(0, 0, 5.6);

const controls = new OrbitControls(camera, canvas);
controls.enableDamping = true;
controls.dampingFactor = 0.055;
controls.enablePan = true;
controls.screenSpacePanning = true;
controls.minDistance = 2.3;
controls.maxDistance = 9;
controls.autoRotate = !reducedMotion;
controls.autoRotateSpeed = 1.25;
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

function traceAlpha(image, targetSize = 150) {
  const sourceCanvas = document.createElement("canvas");
  const ratio = Math.min(targetSize / image.naturalWidth, targetSize / image.naturalHeight);
  const width = Math.max(2, Math.round(image.naturalWidth * ratio));
  const height = Math.max(2, Math.round(image.naturalHeight * ratio));
  sourceCanvas.width = width;
  sourceCanvas.height = height;
  const context = sourceCanvas.getContext("2d", { willReadFrequently: true });
  context.drawImage(image, 0, 0, width, height);
  const pixels = context.getImageData(0, 0, width, height).data;
  const occupied = new Uint8Array(width * height);
  let minX = width, minY = height, maxX = 0, maxY = 0;
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      if (pixels[(y * width + x) * 4 + 3] > 24) {
        occupied[y * width + x] = 1;
        minX = Math.min(minX, x); maxX = Math.max(maxX, x);
        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
      }
    }
  }
  const cropW = maxX - minX + 1, cropH = maxY - minY + 1;
  if (cropW < 2 || cropH < 2) throw new Error("O arquivo do símbolo não possui transparência utilizável.");

  const has = (x, y) => x >= 0 && y >= 0 && x < width && y < height && occupied[y * width + x] === 1;
  const edges = [];
  for (let y = minY; y <= maxY; y++) {
    for (let x = minX; x <= maxX; x++) {
      if (!has(x, y)) continue;
      if (!has(x, y - 1)) edges.push([[x, y], [x + 1, y]]);
      if (!has(x + 1, y)) edges.push([[x + 1, y], [x + 1, y + 1]]);
      if (!has(x, y + 1)) edges.push([[x + 1, y + 1], [x, y + 1]]);
      if (!has(x - 1, y)) edges.push([[x, y + 1], [x, y]]);
    }
  }

  const key = point => point[0] + "," + point[1];
  const starts = new Map();
  edges.forEach((edge, index) => {
    const id = key(edge[0]);
    if (!starts.has(id)) starts.set(id, []);
    starts.get(id).push(index);
  });
  const used = new Uint8Array(edges.length);
  const loops = [];
  for (let i = 0; i < edges.length; i++) {
    if (used[i]) continue;
    const points = [];
    let current = i, guard = 0;
    while (!used[current] && guard++ < edges.length + 2) {
      used[current] = 1;
      const edge = edges[current];
      points.push(edge[0]);
      const next = starts.get(key(edge[1])) || [];
      current = next.find(index => !used[index]);
      if (current === undefined) break;
      if (key(edges[current][0]) === key(points[0]) && key(edges[current][1]) === key(edge[0])) break;
    }
    if (points.length >= 8) loops.push(points);
  }

  const aspect = cropW / cropH;
  const paths = loops.map(points => {
    const vectors = points.map(([x, y]) => new THREE.Vector2(
      ((x - minX) / cropW - 0.5) * aspect,
      (0.5 - (y - minY) / cropH)
    ));
    const simplified = [];
    for (let i = 0; i < vectors.length; i++) {
      const prev = vectors[(i + vectors.length - 1) % vectors.length];
      const curr = vectors[i];
      const next = vectors[(i + 1) % vectors.length];
      const cross = (curr.x - prev.x) * (next.y - curr.y) - (curr.y - prev.y) * (next.x - curr.x);
      if (Math.abs(cross) > 0.00002) simplified.push(curr);
    }
    return simplified;
  }).filter(points => points.length >= 6);

  paths.sort((a, b) => Math.abs(THREE.ShapeUtils.area(b)) - Math.abs(THREE.ShapeUtils.area(a)));
  return { paths, aspect };
}

function makeModel(image) {
  const { paths, aspect } = traceAlpha(image);
  if (!paths.length) throw new Error("Não foi possível criar o contorno 3D do símbolo.");
  const shapes = paths.map(points => {
    const shape = new THREE.Shape();
    shape.moveTo(points[0].x, points[0].y);
    for (let i = 1; i < points.length; i++) shape.lineTo(points[i].x, points[i].y);
    shape.closePath();
    return shape;
  });

  const depth = 0.28;
  const geometry = new THREE.ExtrudeGeometry(shapes, {
    depth,
    bevelEnabled: true,
    bevelSegments: 4,
    steps: 1,
    bevelSize: 0.035,
    bevelThickness: 0.035,
    curveSegments: 2
  });
  geometry.computeVertexNormals();

  const sideMaterial = new THREE.MeshStandardMaterial({
    color: 0x0754a8, metalness: 0.82, roughness: 0.24
  });
  const faceMaterial = new THREE.MeshStandardMaterial({
    color: 0x168cff, metalness: 0.42, roughness: 0.26
  });
  const solid = new THREE.Mesh(geometry, [sideMaterial, faceMaterial]);
  modelRoot.add(solid);

  const textureCanvas = document.createElement("canvas");
  textureCanvas.width = image.naturalWidth;
  textureCanvas.height = image.naturalHeight;
  const ctx = textureCanvas.getContext("2d");
  ctx.drawImage(image, 0, 0);
  const texture = new THREE.CanvasTexture(textureCanvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.anisotropy = renderer.capabilities.getMaxAnisotropy();
  const front = new THREE.Mesh(
    new THREE.PlaneGeometry(aspect, 1),
    new THREE.MeshBasicMaterial({ map: texture, transparent: true, depthWrite: false, side: THREE.DoubleSide })
  );
  front.position.z = depth + 0.041;
  modelRoot.add(front);

  modelRoot.rotation.set(-0.08, -0.22, 0);
  const box = new THREE.Box3().setFromObject(modelRoot);
  const center = box.getCenter(new THREE.Vector3());
  modelRoot.position.sub(center);
}

const image = new Image();
image.crossOrigin = "anonymous";
image.onload = () => {
  try {
    makeModel(image);
    stage.classList.add("is-ready");
  } catch (error) {
    console.error("Falha ao montar símbolo 3D MORFY:", error);
    stage.classList.add("viewer-fallback");
  }
};
image.onerror = () => {
  console.error("Não foi possível carregar o símbolo oficial MORFY.");
  stage.classList.add("viewer-fallback");
};
image.src = "./assets/images/morfy-official-symbol-transparent.png";

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
      camera.position.set(0, 0, 5.6);
      controls.target.set(0, 0, 0);
      modelRoot.rotation.set(-0.08, -0.22, 0);
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
