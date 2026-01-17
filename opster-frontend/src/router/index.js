import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import Project from '../views/Project.vue'
import Server from '../views/Server.vue'
import Service from '../views/Service.vue'
import Monitor from '../views/Monitor.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard
      },
      {
        path: 'project',
        name: 'Project',
        component: Project
      },
      {
        path: 'server',
        name: 'Server',
        component: Server
      },
      {
        path: 'service',
        name: 'Service',
        component: Service
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: Monitor
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
