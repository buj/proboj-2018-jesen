import * as PIXI from 'pixi.js'
import electron from 'electron'
import fs from 'fs'
import path from 'path'
import {debounce, forEach} from 'lodash'

const MAX_CELL_SIZE = 100

const TILES = {
  PLAIN: 0,
  FOREST: 1,
  WATER: 2,
}

const UNIT_TYPES = {
  WARRIOR: 0,
  ARCHER: 1,
}

const TILE_TEXTURES = {
  [TILES.PLAIN]: PIXI.Texture.fromImage('../assets/plain.jpeg'),
  [TILES.FOREST]: PIXI.Texture.fromImage('../assets/forest1.jpeg'),
  [TILES.WATER]: PIXI.Texture.fromImage('../assets/water3.jpg'),
}

const PLAYER_COLORS = [255, 16711680]

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
  currentRound: 0,
  nextStateFraction: 0,
  speed: 1,
  unitGraphics: {},
  pixiApp: null,
  unitsContainer: null,
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
        const st = {}
        st.round = tokens[pos++]
        st.score = tokens[pos++]
        st.isFinalRound = tokens[pos++]

        st.unitCount = tokens[pos++]
        st.units = []
        for (let i = 0; i < st.unitCount; i++) {
          st.units.push({
            x: tokens[pos++],
            y: tokens[pos++],
            id: tokens[pos++],
            owner: tokens[pos++],
            type: tokens[pos++],
            hp: tokens[pos++],
            stamina: tokens[pos++],
          })
        }
        state.states.push(st)
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

const renderMapTiles = () => {
  const {cellSize, n, m, terrain, heights, pixiApp} = state
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

const setEventListeners = () => {
  window.addEventListener(
    'resize',
    debounce(() => {
      // eslint-disable-next-line
      rerenderUI()
    }, 100)
  )
}

const createPixiApp = () => {
  state.pixiApp = new PIXI.Application(state.width, state.height, {
    powerPreference: 'high-performance',
  })
}

const tick = (tickDelta) => {
  const {states, currentRound, unitGraphics, speed, cellSize} = state
  if (states[currentRound].isFinalRound) {
    console.log('FINAL ROUND ENDED')
    return
  }
  const diff = {}
  states[currentRound].units.forEach((unit) => {
    diff[unit.id] = {
      x: unit.x * cellSize,
      y: unit.y * cellSize,
      rawX: unit.x * cellSize,
      rawY: unit.y * cellSize,
      type: unit.type,
      delta: false,
    }
  })
  const next = state.nextStateFraction + tickDelta * speed
  states[currentRound + Math.floor(next)].units.forEach((unit) => {
    diff[unit.id].x -= unit.x * cellSize
    diff[unit.id].y -= unit.y * cellSize
    diff[unit.id].delta = true
  })

  forEach(diff, ({x, y, delta, rawX, rawY, type}, id) => {
    if (!delta) {
      state.unitsContainer.removeChild(unitGraphics[id])
      delete unitGraphics[id]
    } else {
      //console.log(id, unitGraphics[id].x, unitGraphics[id].y, {x, y})
      const centering = type === UNIT_TYPES.ARCHER ? cellSize / 6 : 0
      unitGraphics[id].x = rawX - next * x + centering
      unitGraphics[id].y = rawY - next * y + centering
      if (x !== 0) state.speed = 0.05
    }
  })
  state.nextStateFraction = next
  if (Math.floor(next) >= 1) {
    state.nextStateFraction = 0
    console.log(state.currentRound)
    // must be accessed through state
    state.currentRound += Math.floor(next)
  }
}

const updateCellSize = () => {
  state.cellSize = Math.min(
    window.innerWidth / state.m,
    window.innerHeight / state.n,
    MAX_CELL_SIZE
  )
}

const renderUnits = () => {
  state.unitsContainer = new PIXI.Container()
  const {currentRound, states, cellSize, pixiApp} = state
  state.unitGraphics = {}

  states[currentRound].units.forEach((unit) => {
    const g = new PIXI.Graphics()
    g.beginFill(PLAYER_COLORS[unit.owner])
    if (unit.type === UNIT_TYPES.WARRIOR) {
      g.drawCircle(cellSize / 2, cellSize / 2, cellSize / 3)
      g.x = cellSize * unit.x
      g.y = cellSize * unit.y
    } else {
      g.drawRect(0, 0, (cellSize * 2) / 3, (cellSize * 2) / 3)
      const free = cellSize / 3
      g.x = cellSize * unit.x + free / 2
      g.y = cellSize * unit.y + free / 2
    }
    state.unitGraphics[unit.id] = g
    state.unitsContainer.addChild(g)
  })
  pixiApp.stage.addChild(state.unitsContainer)
}

const rerenderUI = () => {
  state.pixiApp.stage.removeChildren()
  updateCellSize()
  renderMapTiles()
  setEventListeners()
  renderUnits()
}

const createObserver = async (rootElement) => {
  await readObserverLog()
  createPixiApp()
  rerenderUI()

  // game loop
  state.pixiApp.ticker.add((delta) => {
    tick(delta)
  })
  rootElement.appendChild(state.pixiApp.view)
}

export default createObserver
