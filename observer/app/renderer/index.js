import createObserver from './observer'
import Stats from 'stats.js'

const rootElement = document.querySelector(document.currentScript.getAttribute('data-container'))
setTimeout(async () => {
  createObserver(rootElement)
  await document.getElementById('loading').classList.toggle('visible')
}, 1500)

if (process.env.NODE_ENV === 'development') {
  const stats = new Stats()
  stats.showPanel(0) // 0: fps, 1: ms, 2: mb, 3+: custom
  document.body.appendChild(stats.dom)

  window.requestAnimationFrame(function loop() {
    stats.update()
    window.requestAnimationFrame(loop)
  })
}
