import {spawn} from 'child_process'
import electron from 'electron'
import browserSync from 'browser-sync'
import browserSyncConnectUtils from 'browser-sync/dist/connect-utils'
import path from 'path'

const bsync = browserSync.create()

const getRootUrl = (options) => {
  const port = options.get('port')
  return `http://localhost:${port}`
}

const getClientUrl = (options) => {
  const pathname = browserSyncConnectUtils.clientScript(options)
  return getRootUrl(options) + pathname
}

bsync.init(
  {
    ui: false,
    // Port 35829 = LiveReload's default port 35729 + 100.
    // If the port is occupied, Browsersync uses next free port automatically.
    port: 35829,
    ghostMode: false,
    open: false,
    notify: false,
    logSnippet: false,
    socket: {
      // Use the actual port here.
      domain: getRootUrl,
    },
  },
  (err, bs) => {
    if (err) {
      // eslint-disable-next-line
      console.error(err)
      return
    }

    // NOTE: hardcoded observer file
    const child = spawn(
      electron,
      ['.', path.join(__dirname, '../observer.log'), path.join(__dirname, '../names.log')],
      {
        env: {
          ...{
            NODE_ENV: 'development',
            BROWSER_SYNC_CLIENT_URL: getClientUrl(bs.options),
          },
          ...process.env,
        },
        stdio: 'inherit',
      }
    )

    child.on('close', () => {
      process.exit()
    })

    bsync.watch('build/**/*').on('change', bsync.reload)
  }
)
