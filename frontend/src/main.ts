import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import './assets/main.css'
import './assets/admin-page.css'
import 'katex/dist/katex.min.css'
import 'markdown-it-texmath/css/texmath.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
