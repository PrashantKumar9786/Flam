// Get DOM elements
const processedImg = document.getElementById('processed-frame') as HTMLImageElement | null;
const rawImg = document.getElementById('raw-frame') as HTMLImageElement | null;
const toggleBtn = document.getElementById('toggleBtn') as HTMLButtonElement | null;
const processedView = document.getElementById('processed-view');
const rawView = document.getElementById('raw-view');
const statsDiv = document.getElementById('stats');

// Track current view state
let showingProcessed = true;

// Update stats with image metadata
function updateStats() {
  if (!statsDiv) return;
  
  const currentImg = showingProcessed ? processedImg : rawImg;
  const res = currentImg && currentImg.naturalWidth && currentImg.naturalHeight 
    ? `${currentImg.naturalWidth}x${currentImg.naturalHeight}` 
    : 'unknown';
  
  const fps = '~12 fps (sample)';
  statsDiv.textContent = `Resolution: ${res} | FPS: ${fps}`;
}

// Toggle between processed and raw views
function toggleView() {
  if (!processedView || !rawView || !toggleBtn) return;
  
  showingProcessed = !showingProcessed;
  
  if (showingProcessed) {
    processedView.style.display = 'block';
    rawView.style.display = 'none';
    toggleBtn.textContent = 'Show Raw Camera Feed';
  } else {
    processedView.style.display = 'none';
    rawView.style.display = 'block';
    toggleBtn.textContent = 'Show Processed Frame';
  }
  
  updateStats();
}

document.addEventListener('DOMContentLoaded', () => {
  // Set up toggle button
  if (toggleBtn) {
    toggleBtn.addEventListener('click', toggleView);
  }
  
  // Update stats when images load
  if (processedImg) {
    if (processedImg.complete) updateStats();
    processedImg.addEventListener('load', updateStats);
  }
  
  if (rawImg) {
    rawImg.addEventListener('load', () => {
      if (!showingProcessed) updateStats();
    });
  }
  
  // Initial stats update
  updateStats();
});
