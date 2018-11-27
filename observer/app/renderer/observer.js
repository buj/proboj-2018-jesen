import * as PIXI from 'pixi.js'
import electron from 'electron'
import fs from 'fs'
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
  [TILES.FOREST]: PIXI.Texture.fromImage('../assets/forest2.jpeg'),
  [TILES.WATER]: PIXI.Texture.fromImage('../assets/water2.jpg'),
}

const UNIT_TEXTURES = {
  [UNIT_TYPES.WARRIOR]: [
    PIXI.Texture.fromImage('../assets/blue_sword.png'),
    PIXI.Texture.fromImage('../assets/red_sword.png'),
  ],
  [UNIT_TYPES.ARCHER]: [
    PIXI.Texture.fromImage('../assets/blue_bow.png'),
    PIXI.Texture.fromImage('../assets/red_bow.png'),
  ],
}

const PLAYER_COLORS = [255, 16711680]
const SPEED_STEP = 0.05

const state = {
  // constants
  width: 1000,
  height: 700,
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
  speed: 0.2,
  unitGraphics: {},
  pixiApp: null,
  unitsContainer: null,
}

const updateCellSize = () => {
  state.cellSize = Math.min(
    window.innerWidth / state.m,
    window.innerHeight / state.n,
    MAX_CELL_SIZE
  )
}

const readObserverLog = () => {
  return new Promise((res, rej) => {
    const args = electron.remote.process.argv
    // NOTE: in development the observer file is specified as third argument
    if (args.length < 2) {
      const a = JSON.stringify(args)
      rej(`${a}Observer log file not specified!`)
      return
    }

    fs.readFile(args[1] === '.' ? args[2] : args[1], 'utf-8', (err, data) => {
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
      updateCellSize()
      res()
    })
  })
}

const renderMapTiles = () => {
  const {cellSize, n, m, terrain, heights, pixiApp} = state
  const terrainContainer = new PIXI.Container()
  for (let i = 0; i < n; i++) {
    for (let j = 0; j < m; j++) {
      const texture = TILE_TEXTURES[terrain[i][j]]
      const tile = new PIXI.extras.TilingSprite(texture, cellSize, cellSize)
      tile.tileScale = new PIXI.Point(cellSize / texture.width, cellSize / texture.height)
      const mask = new PIXI.Graphics()
      mask.beginFill(0, heights[i][j] / 5)
      mask.drawRect(0, 0, cellSize, cellSize)
      tile.x = mask.x = cellSize * i
      tile.y = mask.y = cellSize * j
      terrainContainer.addChild(tile)
      terrainContainer.addChild(mask)
    }
  }
  pixiApp.renderer.resize(cellSize * n, cellSize * m)
  terrainContainer.cacheAsBitmap = true
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
  window.addEventListener('keydown', (e) => {
    switch (e.key) {
      case '+':
        state.speed = Math.min(state.speed + SPEED_STEP, 1)
        break
      case '-':
        state.speed = Math.max(state.speed - SPEED_STEP, 0)
        break
      case ' ':
        if (state.speed > 0) {
          state.savedSpeed = state.speed
          state.speed = 0
        } else {
          if (state.speed === 0) state.speed = state.savedSpeed
          state.savedSpeed = undefined
        }
        break
      default:
        break
    }
    document.getElementById('speed').innerHTML = Math.round(state.speed * 100) / 100
  })
}

const createPixiApp = () => {
  state.pixiApp = new PIXI.Application(state.width, state.height, {
    powerPreference: 'high-performance',
  })
  state.pixiApp.stage.interactiveChildren = true
}

const tick = (tickDelta) => {
  const {states, currentRound, unitGraphics, speed, cellSize} = state
  document.getElementById('score').innerHTML = states[currentRound].score
  if (states[currentRound].isFinalRound) {
    document.getElementById('modal').classList.add('visible')
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
  state.nextStateFraction = Math.min(1, state.nextStateFraction + tickDelta * speed)
  states[currentRound + 1].units.forEach((unit) => {
    diff[unit.id].x -= unit.x * cellSize
    diff[unit.id].y -= unit.y * cellSize
    diff[unit.id].delta = true
  })

  forEach(diff, ({x, y, delta, rawX, rawY, type}, id) => {
    if (!delta && state.nextStateFraction >= 1) {
      state.unitsContainer.removeChild(unitGraphics[id])
      unitGraphics[id].destroy()
      delete unitGraphics[id]
    } else {
      //console.log(id, unitGraphics[id].x, unitGraphics[id].y, {x, y})
      unitGraphics[id].x = rawX - state.nextStateFraction * x
      unitGraphics[id].y = rawY - state.nextStateFraction * y
    }
  })
  if (state.nextStateFraction >= 1) {
    state.nextStateFraction = 0
    document.getElementById('round').innerHTML = currentRound
    // must be accessed through state
    state.currentRound += 1
  }
}

const renderUnits = () => {
  state.unitsContainer = new PIXI.Container()
  const {currentRound, states, cellSize, pixiApp} = state
  state.unitGraphics = {}

  states[currentRound].units.forEach((unit) => {
    const texture = UNIT_TEXTURES[unit.type][unit.owner]
    const tile = new PIXI.extras.TilingSprite(texture, cellSize, cellSize)
    tile.tileScale = new PIXI.Point(cellSize / texture.width, cellSize / texture.height)
    tile.x = cellSize * unit.x
    tile.y = cellSize * unit.y
    state.unitGraphics[unit.id] = tile
    state.unitsContainer.addChild(tile)
  })
  pixiApp.stage.addChild(state.unitsContainer)
}

const rerenderUI = () => {
  state.pixiApp.stage.removeChildren()
  updateCellSize()
  renderMapTiles()
  renderUnits()
}

const createObserver = async (rootElement) => {
  try {
    await readObserverLog()
    setEventListeners()
    document.getElementById('speed').innerHTML = Math.round(state.speed * 100) / 100
    createPixiApp()
    rerenderUI()

    rootElement.appendChild(state.pixiApp.view)
    // game loop
    state.pixiApp.ticker.add((delta) => {
      tick(delta)
    })
  } catch (err) {
    const errorElem = document.getElementById('error')
    errorElem.innerHTML = err
    errorElem.classList.add('visible')
  }
}

export default createObserver
