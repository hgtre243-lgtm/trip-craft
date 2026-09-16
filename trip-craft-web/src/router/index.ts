import { createRouter, createWebHistory } from 'vue-router'
import MapContainer from '../components/MapContainer.vue'
import FootprintBoard from '../components/FootprintBoard.vue'
import TripPlanner from '../components/TripPlanner.vue'

const routes = [
    { path: '/', name: 'map', component: MapContainer },
    { path: '/footprint', name: 'footprint', component: FootprintBoard },
    { path: '/planner', name: 'planner', component: TripPlanner },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

export default router
