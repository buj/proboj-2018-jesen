import {setIn, getIn} from 'imuty'
import getInitialState from '../initialState'

const rootReducer = (state = getInitialState(), action) => {
  const {reducer, path, payload} = action
  // Fallback for actions from different sources
  if (!reducer) return state
  const updatedState = reducer(getIn(state, path || []), payload)
  return setIn(state, path || [], updatedState)
}

export default rootReducer
