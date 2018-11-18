import {setIn} from 'imuty'

export const updateValue = (path, data, type) => ({
  type: type || `Update state in [${path}]`,
  payload: data,
  reducer: (state) => setIn(state, path, data),
})
