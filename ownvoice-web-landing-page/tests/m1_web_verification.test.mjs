import test from "node:test";
import assert from "node:assert/strict";
import fs from "fs";
import path from "path";

test("Binary streaming path resolution locates releases from both subfolder and root execution contexts", () => {
  // Scenario A: process.cwd() is inside ownvoice-web-landing-page
  const cwdSubfolder = process.cwd().endsWith("ownvoice-web-landing-page")
    ? process.cwd()
    : path.join(process.cwd(), "ownvoice-web-landing-page");

  const subfolderDirs = [
    path.join(cwdSubfolder, "private_storage", "installers"),
    path.join(cwdSubfolder, "ownvoice-web-landing-page", "private_storage", "installers"),
    path.join(cwdSubfolder, "releases"),
    path.join(cwdSubfolder, "..", "releases"),
    path.join(cwdSubfolder, "dist"),
    path.join(cwdSubfolder, "..", "dist"),
  ];

  let subfolderWin = null;
  let subfolderAndroid = null;
  for (const dir of subfolderDirs) {
    if (!fs.existsSync(dir)) continue;
    const winCandidate = path.join(dir, "OwnVoice-v2.0.0-Windows.zip");
    if (fs.existsSync(winCandidate)) subfolderWin = winCandidate;
    const apkCandidate = path.join(dir, "OwnVoice-v3.0.0.apk");
    if (fs.existsSync(apkCandidate)) subfolderAndroid = apkCandidate;
  }
  assert.ok(subfolderWin, "Must locate OwnVoice-v2.0.0-Windows.zip from subfolder execution");
  assert.ok(subfolderAndroid, "Must locate OwnVoice-v3.0.0.apk from subfolder execution");

  // Scenario B: process.cwd() is at project root
  const cwdRoot = process.cwd().endsWith("ownvoice-web-landing-page")
    ? path.join(process.cwd(), "..")
    : process.cwd();

  const rootDirs = [
    path.join(cwdRoot, "private_storage", "installers"),
    path.join(cwdRoot, "ownvoice-web-landing-page", "private_storage", "installers"),
    path.join(cwdRoot, "releases"),
    path.join(cwdRoot, "..", "releases"),
    path.join(cwdRoot, "dist"),
    path.join(cwdRoot, "..", "dist"),
  ];

  let rootWin = null;
  let rootAndroid = null;
  for (const dir of rootDirs) {
    if (!fs.existsSync(dir)) continue;
    const winCandidate = path.join(dir, "OwnVoice-v2.0.0-Windows.zip");
    if (fs.existsSync(winCandidate)) rootWin = winCandidate;
    const apkCandidate = path.join(dir, "OwnVoice-v3.0.0.apk");
    if (fs.existsSync(apkCandidate)) rootAndroid = apkCandidate;
  }
  assert.ok(rootWin, "Must locate OwnVoice-v2.0.0-Windows.zip from root execution");
  assert.ok(rootAndroid, "Must locate OwnVoice-v3.0.0.apk from root execution");
});

test("Docs slug database contains changelog, getting-started, privacy, and user-guide with zero missing entries", async () => {
  const docsPath = path.join(
    process.cwd().endsWith("ownvoice-web-landing-page") ? process.cwd() : path.join(process.cwd(), "ownvoice-web-landing-page"),
    "app", "(marketing)", "docs", "[slug]", "page.tsx"
  );
  const content = fs.readFileSync(docsPath, "utf-8");

  assert.ok(content.includes('"changelog":'), "DOCS_DATABASE must contain 'changelog'");
  assert.ok(content.includes('"getting-started":'), "DOCS_DATABASE must contain 'getting-started'");
  assert.ok(content.includes('"privacy":'), "DOCS_DATABASE must contain 'privacy'");
  assert.ok(content.includes('"user-guide":'), "DOCS_DATABASE must contain 'user-guide'");
  assert.ok(content.includes("Version 2.0.0 (Windows Desktop Hub)"), "Changelog must detail Windows v2.0.0");
  assert.ok(content.includes("Version 3.0.0 (Android Keyboard & Companion)"), "Changelog must detail Android v3.0.0");
});

test("Marketing components use authentic Windows controls and obsidian tokens with zero macOS artifacts", () => {
  const basePath = process.cwd().endsWith("ownvoice-web-landing-page")
    ? process.cwd()
    : path.join(process.cwd(), "ownvoice-web-landing-page");

  const heroPath = path.join(basePath, "components", "marketing", "HeroVisual.tsx");
  const heroContent = fs.readFileSync(heroPath, "utf-8");

  // Zero macOS symbols in HeroVisual
  assert.equal(heroContent.includes("⌘"), false, "HeroVisual must not contain macOS ⌘ glyph");
  assert.equal(heroContent.includes("bg-red-500/80"), false, "HeroVisual titlebar must not have red traffic light");
  assert.equal(heroContent.includes("bg-yellow-500/80"), false, "HeroVisual titlebar must not have yellow traffic light");
  assert.ok(heroContent.includes("Ctrl+Space"), "HeroVisual must feature Ctrl+Space token");
  assert.ok(heroContent.includes("F8"), "HeroVisual must feature F8 shortcut token");
  assert.ok(heroContent.includes("─") && heroContent.includes("□") && heroContent.includes("✕"), "HeroVisual must have Windows controls");

  // WorkspaceShowcase
  const workspacePath = path.join(basePath, "components", "marketing", "WorkspaceShowcase.tsx");
  const workspaceContent = fs.readFileSync(workspacePath, "utf-8");
  assert.equal(workspaceContent.includes("bg-red-500/80"), false, "WorkspaceShowcase titlebar must not have red traffic light");
  assert.ok(workspaceContent.includes("─") && workspaceContent.includes("□") && workspaceContent.includes("✕"), "WorkspaceShowcase must have Windows controls");

  // ProviderRibbon id="providers"
  const ribbonPath = path.join(basePath, "components", "marketing", "ProviderRibbon.tsx");
  const ribbonContent = fs.readFileSync(ribbonPath, "utf-8");
  assert.ok(ribbonContent.includes('id="providers"'), "ProviderRibbon must have id='providers' anchor target");

  // Lookup Page: no macOS download button
  const lookupPath = path.join(basePath, "app", "download", "lookup", "page.tsx");
  const lookupContent = fs.readFileSync(lookupPath, "utf-8");
  assert.equal(lookupContent.includes("os=mac"), false, "Lookup page must not expose phantom macOS download button");
  assert.ok(lookupContent.includes("os=windows"), "Lookup page must offer Windows download");
  assert.ok(lookupContent.includes("os=android"), "Lookup page must offer Android download");

  // MarketingFooter: zero 404 links
  const footerPath = path.join(basePath, "components", "marketing", "MarketingFooter.tsx");
  const footerContent = fs.readFileSync(footerPath, "utf-8");
  assert.equal(footerContent.includes('href="/about"'), false, "MarketingFooter must not link to non-existent /about");
  assert.equal(footerContent.includes('href="/docs/user-guide"'), false, "MarketingFooter must route User Guide to /docs/getting-started");
  assert.ok(footerContent.includes('href="/contact"'), "MarketingFooter must link About Us to /contact");
  assert.ok(footerContent.includes('href="/docs/getting-started"'), "MarketingFooter must link to /docs/getting-started");
  assert.ok(footerContent.includes('href="/docs/privacy"'), "MarketingFooter must link to /docs/privacy");
});

test("Root scripts consolidate onto ownvoice-web-landing-page", () => {
  const rootDir = process.cwd().endsWith("ownvoice-web-landing-page")
    ? path.join(process.cwd(), "..")
    : process.cwd();

  const batContent = fs.readFileSync(path.join(rootDir, "run_web.bat"), "utf-8");
  assert.ok(batContent.includes("cd ownvoice-web-landing-page"), "Root run_web.bat must enter ownvoice-web-landing-page");
  assert.equal(batContent.includes("cd web\r\n") || batContent.includes("cd web\n"), false, "Root run_web.bat must not target legacy web directory");

  const pkgContent = JSON.parse(fs.readFileSync(path.join(rootDir, "package.json"), "utf-8"));
  for (const [key, script] of Object.entries(pkgContent.scripts)) {
    assert.ok(script.includes("--prefix ownvoice-web-landing-page"), `Root script '${key}' must target ownvoice-web-landing-page`);
    assert.equal(script.includes("--prefix web "), false, `Root script '${key}' must not target legacy web directory`);
  }
});
