import React from 'react'
import {withState, compose} from 'recompose'
import {updateValue as _updateValue} from './actions'
import {connect} from 'react-redux'
import electron from 'electron'

const App = ({count, setCount, cnt, updateValue}) => {
  return [
    <button key="1" onClick={() => setCount(count + 1)}>
      {count}
    </button>,
    <button key="2" onClick={() => updateValue(['cnt'], cnt + 1)}>
      {cnt}
    </button>,
    <p key="3">{JSON.stringify(electron.remote.process.argv)}</p>,
  ]
}

export default compose(
  connect(
    (state) => ({
      ...state,
    }),
    {updateValue: _updateValue}
  ),
  withState('count', 'setCount', 0)
)(App)
