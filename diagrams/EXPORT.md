Exporting Mermaid (.mmd) diagrams to PNG

This folder contains Mermaid source files (*.mmd). Use the commands below to render them to PNG images.

Quick (no install, uses npx):

```bash
npx --yes @mermaid-js/mermaid-cli -i diagrams/cognitive-loop.mmd -o diagrams/cognitive-loop.png
```

Render all `.mmd` files in `diagrams/` to PNG (macOS / Linux):

```bash
for f in diagrams/*.mmd; do
  npx --yes @mermaid-js/mermaid-cli -i "$f" -o "${f%.mmd}.png"
done
```

Install globally (optional):

```bash
npm install -g @mermaid-js/mermaid-cli
# then
mmdc -i diagrams/cognitive-loop.mmd -o diagrams/cognitive-loop.png
```

Notes
- The CLI will automatically download required dependencies (Puppeteer) when needed.
- If you prefer Docker, install the mermaid-cli image and adapt the commands accordingly.
- After rendering, add the generated PNG files to git if you want them tracked.

Suggested git steps after exporting:

```bash
git add diagrams/*.mmd diagrams/*.png
git commit -m "Add exported diagram images"
git push origin main
```
