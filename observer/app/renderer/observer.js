import * as PIXI from 'pixi.js'
import electron from 'electron'
import fs from 'fs'
import {debounce, forEach} from 'lodash'
import {transposeState} from './transpose'

const MAX_CELL_SIZE = 500

const MODE = {
  OBSERVER: -1,
  PLAYER1: 0,
  PLAYER2: 1,
}

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
  [TILES.PLAIN]: PIXI.Texture.fromImage('../assets/snow2.jpeg'),
  [TILES.FOREST]: PIXI.Texture.fromImage('../assets/forest3.jpg'),
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

const FOG_TEXTURE = PIXI.Texture.fromImage('../assets/fog.png')
const WALL_TEXTURE = PIXI.Texture.fromImage('../assets/ice_wall.jpg')

const SPEED_STEP = 0.05
const ZOOM_DELTA = 0.05
const CAMERA_MOVE_DELTA = 50

let state = {
  zoom: 1,
  stageOffset: {
    x: 0,
    y: 0,
  },
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
    window.innerWidth / state.n,
    window.innerHeight / state.m,
    MAX_CELL_SIZE
  )
}

const updateRendererSize = () => {
  const {pixiApp, cellSize, n, m, zoom, stageOffset} = state
  pixiApp.renderer.resize(cellSize * n * zoom + stageOffset.x, cellSize * m * zoom + stageOffset.y)
}

const updateZoom = (delta) => {
  state.zoom = Math.max(0, state.zoom + delta)
  state.pixiApp.stage.scale = new PIXI.Point(state.zoom, state.zoom)
  document.getElementById('zoom').innerHTML = Math.round(state.zoom * 100) / 100
  updateRendererSize()
}

const updateStageCenter = (xDelta, yDelta) => {
  const {cellSize, n, m, zoom, stageOffset} = state
  if (state.stageOffset.x + xDelta > 0 || state.stageOffset.y + yDelta > 0) return
  const hiddenWidth = cellSize * m * zoom - window.innerWidth
  const hiddenHeight = cellSize * n * zoom - window.innerHeight
  if (xDelta < 0 && -(stageOffset.x + xDelta) > hiddenWidth) return
  if (yDelta < 0 && -(stageOffset.y + yDelta) > hiddenHeight) return
  state.stageOffset.x += xDelta
  state.stageOffset.y += yDelta
  state.pixiApp.stage.x += xDelta
  state.pixiApp.stage.y += yDelta
  updateRendererSize()
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
            const x = tokens[pos++]
            // NOTE: so we don't have to tranpose later
            vis.push([tokens[pos++], x])
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

      // other initialization
      state.n += 1
      state = transposeState(state)
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
      const texture = j === m - 1 ? WALL_TEXTURE : TILE_TEXTURES[terrain[i][j]]
      const tile = new PIXI.extras.TilingSprite(texture, cellSize, cellSize)
      tile.tileScale = new PIXI.Point(cellSize / texture.width, cellSize / texture.height)
      tile.x = cellSize * i
      tile.y = cellSize * j
      terrainContainer.addChild(tile)
      if (j !== m - 1) {
        const mask = new PIXI.Graphics()
        mask.x = cellSize * i
        mask.y = cellSize * j
        mask.beginFill(0, heights[i][j] / 5)
        mask.drawRect(0, 0, cellSize, cellSize)
        terrainContainer.addChild(mask)
      }
    }
  }
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
      case '0':
        state.mapType = MODE.OBSERVER
        break
      case '1':
        state.mapType = MODE.PLAYER1
        break
      case '2':
        state.mapType = MODE.PLAYER2
        break
      case '4':
        updateZoom(ZOOM_DELTA)
        break
      case '5':
        updateZoom(-ZOOM_DELTA)
        break
      case 'ArrowLeft':
        updateStageCenter(CAMERA_MOVE_DELTA, 0)
        break
      case 'ArrowRight':
        updateStageCenter(-CAMERA_MOVE_DELTA, 0)
        break
      case 'ArrowUp':
        updateStageCenter(0, CAMERA_MOVE_DELTA)
        break
      case 'ArrowDown':
        updateStageCenter(0, -CAMERA_MOVE_DELTA)
        break
      default:
        break
    }
    document.getElementById('speed').innerHTML = Math.round(state.speed * 100) / 100
  })
}

const createPixiApp = () => {
  // TODO: use maximum allowed size
  state.pixiApp = new PIXI.Application(1000, 600, {
    powerPreference: 'high-performance',
  })
  state.pixiApp.stage.interactiveChildren = true
}

const renderFogOfWar = () => {
  const {currentRound, states, cellSize, pixiApp, mapType, n, m, visibility} = state
  if (state.fogContainer) {
    state.fogContainer.destroy()
    delete state.fogContainer
  }
  if (mapType === MODE.OBSERVER) return
  state.fogContainer = new PIXI.Container()

  const isFog = []
  for (let i = 0; i < n; i++) {
    const row = []
    for (let j = 0; j < m; j++) {
      row.push(true)
    }
    isFog.push(row)
  }

  states[currentRound].units.forEach((unit) => {
    if (unit.owner === state.mapType) {
      visibility[unit.x][unit.y].forEach((cell) => {
        isFog[cell[0]][cell[1]] = false
      })
    }
  })

  for (let i = 0; i < n; i++) {
    for (let j = 0; j < m; j++) {
      if (!isFog[i][j]) continue
      const tile = new PIXI.extras.TilingSprite(FOG_TEXTURE, cellSize, cellSize)
      tile.tileScale = new PIXI.Point(cellSize / FOG_TEXTURE.width, cellSize / FOG_TEXTURE.height)
      tile.x = cellSize * i
      tile.y = cellSize * j
      state.fogContainer.addChild(tile)
    }
  }

  pixiApp.stage.addChild(state.fogContainer)
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
  renderFogOfWar()
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
  updateZoom(0)
  renderMapTiles()
  renderUnits()
  renderFogOfWar()
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
