import * as PIXI from 'pixi.js'

const tiles = {}

const randomPixiColor = () => Math.floor(Math.random() * 16777215)

const createPixiApp = () => {
  const pixiApp = new PIXI.Application(1000, 600, {
    backgroundColor: 0x1099bb,
  })
  return pixiApp
}

const tick = (delta) => {
  for (let i = 0; i < 10000; i++) {
    tiles[i].setTransform(
      tiles[i].x + (Math.random() < 0.5 ? -1 : 1) * 1,
      tiles[i].y + (Math.random() < 0.5 ? -1 : 1) * 1
    )
  }
}

const createObserver = () => {
  const pixiApp = createPixiApp()

  const g = new PIXI.Graphics()
  g.beginFill(randomPixiColor())
  g.drawRect(0, 0, 25, 25)
  const texture = pixiApp.renderer.generateTexture(g)
  for (let i = 0; i < 100000; i++) {
    const s = new PIXI.Sprite(texture)
    pixiApp.stage.addChild(s)
    tiles[i] = s
  }

  // game loop
  pixiApp.ticker.add((delta) => {
    tick(delta)
  })
  return pixiApp
}

export default createObserver
