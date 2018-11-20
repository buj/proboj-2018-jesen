import * as PIXI from 'pixi.js'
import electron from 'electron'
import fs from 'fs'
import path from 'path'
import {debounce, range} from 'lodash'

const MAX_CELL_SIZE = 100

const TILES = {
  PLAIN: 0,
  FOREST: 1,
  WATER: 2,
}

const TILE_TEXTURES = {
  [TILES.PLAIN]: PIXI.Texture.fromImage('../assets/plain.jpeg'),
  [TILES.FOREST]: PIXI.Texture.fromImage('../assets/forest1.jpeg'),
  [TILES.WATER]: PIXI.Texture.fromImage('../assets/water3.jpg'),
}

const tiles = {}
const state = {
  // constants
  width: 500,
  height: 400,
  // these are loaded from observer log file
  mapType: null,
  n: null,
  m: null,
  terrain: [],
  heights: [],
  visibility: [],
  states: [],
  // NOTE: these are derived
  cellSize: null,
}

const readObserverLog = () => {
  return new Promise((res, rej) => {
    if (electron.remote.process.argv.length < 3) {
      rej('Observer log file not specified!')
      return
    }

    const file = path.join(__dirname, '.', electron.remote.process.argv[2])
    fs.readFile(file, 'utf-8', (err, data) => {
      if (err) {
        rej(`Error while reading observer log file: "${err.message}"`)
        return
      }
      const tokens = data.split(/\s+/g).map((x) => +x)
      let pos = 0

      // type
      state.mapType = tokens[pos++]
      // dimensions
      state.n = tokens[pos++]
      state.m = tokens[pos++]

      // terrain
      for (let i = 0; i < state.n; i++) {
        const row = []
        for (let j = 0; j < state.m; j++) {
          row.push(tokens[pos++])
        }
        state.terrain.push(row)
      }

      // heights
      for (let i = 0; i < state.n; i++) {
        const row = []
        for (let j = 0; j < state.m; j++) {
          row.push(tokens[pos++])
        }
        state.heights.push(row)
      }

      // visibility
      for (let i = 0; i < state.n; i++) {
        const row = []
        for (let j = 0; j < state.m; j++) {
          const seeCount = tokens[pos++]
          const vis = []
          for (let k = 0; k < seeCount; k++) {
            vis.push([tokens[pos++], tokens[pos++]])
          }
          row.push(vis)
        }
        state.visibility.push(row)
      }

      // game states
      while (pos < tokens.length) {
        const state = {}
        state.round = tokens[pos++]
        state.score = tokens[pos++]
        state.isFinalRound = tokens[pos++]

        state.unitCount = tokens[pos++]
        state.units = []
        for (let i = 0; i < state.unitCount; i++) {
          state.units.push({
            x: tokens[pos]++,
            y: tokens[pos]++,
            id: tokens[pos]++,
            owner: tokens[pos]++,
            type: tokens[pos]++,
            hp: tokens[pos]++,
            stamina: tokens[pos]++,
          })
        }
      }
      state.cellSize = Math.min(
        window.innerWidth / state.m,
        window.innerHeight / state.n,
        MAX_CELL_SIZE
      )
      res()
    })
  })
}

const renderMapTiles = (pixiApp) => {
  const {cellSize, n, m, terrain, heights} = state
  const terrainContainer = new PIXI.Container()
  for (let i = 0; i < n; i++) {
    for (let j = 0; j < m; j++) {
      const tile = new PIXI.extras.TilingSprite(TILE_TEXTURES[terrain[i][j]], cellSize, cellSize)
      const mask = new PIXI.Graphics()
      mask.beginFill(0, heights[i][j] / 10)
      mask.drawRect(0, 0, cellSize, cellSize)
      tile.x = mask.x = cellSize * i
      tile.y = mask.y = cellSize * j
      terrainContainer.addChild(tile)
      terrainContainer.addChild(mask)
    }
  }
  pixiApp.renderer.resize(cellSize * n, cellSize * m)
  pixiApp.stage.addChild(terrainContainer)
}

const setEventListeners = (pixiApp) => {
  window.addEventListener(
    'resize',
    debounce(() => {
      // eslint-disable-next-line
      rerenderUI(pixiApp)
    }, 100)
  )
}

const createPixiApp = () => {
  const pixiApp = new PIXI.Application(state.width, state.height, {
    powerPreference: 'high-performance',
  })
  return pixiApp
}

const tick = (delta) => {
  for (let i = 0; i < 100; i++) {
    tiles[i].setTransform(
      tiles[i].x + (Math.random() < 0.5 ? -1 : 1) * 1,
      tiles[i].y + (Math.random() < 0.5 ? -1 : 1) * 1
    )
  }
}

const updateCellSize = () => {
  state.cellSize = Math.min(window.innerWidth / state.m, window.innerHeight / state.n)
}

const rerenderUI = (pixiApp) => {
  pixiApp.stage.removeChildren()
  updateCellSize()
  renderMapTiles(pixiApp)
  setEventListeners(pixiApp)
}

const createObserver = async (rootElement) => {
  await readObserverLog()
  const pixiApp = createPixiApp()
  rerenderUI(pixiApp)

  // game loop
  pixiApp.ticker.add((delta) => {
    //tick(delta)
  })
  rootElement.appendChild(pixiApp.view)
}

export default createObserver
