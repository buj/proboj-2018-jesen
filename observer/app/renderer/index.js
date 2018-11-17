import React from 'react'
import ReactDOM from 'react-dom'
import {Provider} from 'react-redux'

import App from './App'
import getInitialState from './initialState'
import configureStore from './store/configureStore'

const store = configureStore(getInitialState())

const rootElement = document.querySelector(document.currentScript.getAttribute('data-container'))

ReactDOM.render(
  <Provider store={store}>
    <App />
  </Provider>,
  rootElement
)
