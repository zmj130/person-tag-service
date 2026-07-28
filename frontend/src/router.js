import { createRouter, createWebHistory } from 'vue-router'
import PersonsView from './views/PersonsView.vue'
import TagsView from './views/TagsView.vue'
import ReviewsView from './views/ReviewsView.vue'
import SyncView from './views/SyncView.vue'
import IndicatorsView from './views/IndicatorsView.vue'
import RulesView from './views/RulesView.vue'
import ImportsView from './views/ImportsView.vue'
import ProfilesView from './views/ProfilesView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/persons' },
    { path: '/persons', component: PersonsView, meta: { title: '人员管理' } },
    { path: '/tags', component: TagsView, meta: { title: '标签管理' } },
    { path: '/reviews', component: ReviewsView, meta: { title: '标签审核' } },
    { path: '/sync', component: SyncView, meta: { title: '同步管理' } },
    { path: '/indicators', component: IndicatorsView, meta: { title: '指标管理' } },
    { path: '/rules', component: RulesView, meta: { title: '规则管理' } },
    { path: '/imports', component: ImportsView, meta: { title: '人员导入' } },
    { path: '/profiles', component: ProfilesView, meta: { title: '用户画像' } }
  ]
})
