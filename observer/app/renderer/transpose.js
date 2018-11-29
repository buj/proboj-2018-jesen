const transpose2D = (array) => array[0].map((col, i) => array.map((row) => row[i]))

export const transposeState = (state) => {
  const {n, m, terrain, heights, visibility, states} = state
  const newState = {}
  newState.m = n
  newState.n = m
  newState.terrain = transpose2D(terrain)
  newState.heights = transpose2D(heights)
  newState.visibility = transpose2D(visibility)
  newState.states = states.map((st) => ({
    ...st,
    units: st.units.map((u) => ({
      ...u,
      x: u.y,
      y: u.x,
    })),
  }))

  return {...state, ...newState}
}
