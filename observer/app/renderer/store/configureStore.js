import thunk from 'redux-thunk'
import {applyMiddleware, createStore} from 'redux'
import {createLogger} from 'redux-logger'
import rootReducer from './rootReducer'

export default (state) => {
  const logger = {
    log: () => null,
  }
  const loggerMiddleware = createLogger({
    collapsed: true,
  })

  const middlewares = [thunk.withExtraArgument({logger})]
  if (process.env.NODE_ENV === 'development') {
    middlewares.push(loggerMiddleware)
  }

  const store = createStore(rootReducer, state, applyMiddleware(...middlewares))

  if (process.env.NODE_ENV === 'development') {
    logger.log = (message, payload) =>
      store.dispatch({
        type: message,
        payload,
      })
  }

  return store
}
