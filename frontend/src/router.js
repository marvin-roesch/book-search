import Vue from 'vue';
import Router from 'vue-router';
import Home from './views/Home.vue';
import NewBook from '@/views/NewBook.vue';
import BookUploader from '@/components/management/wizard/BookUploader.vue';
import TableOfContents from '@/components/management/wizard/TableOfContents.vue';
import MetadataEditor from '@/components/management/wizard/MetadataEditor.vue';
import ClassMapper from '@/components/management/wizard/ClassMapper.vue';
import SearchResults from '@/views/SearchResults.vue';
import Chapter from '@/views/Chapter.vue';
import store from '@/store';
import Login from '@/views/Login.vue';
import UserManagement from '@/views/UserManagement.vue';
import UserProfile from '@/views/UserProfile.vue';

Vue.use(Router);

const withPrefix = (prefix, routes) => routes.map((route) => {
  route.path = prefix + route.path;
  return route;
});

const router = new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: { requiresUnauthorized: true },
    },
    {
      path: '/',
      name: 'home',
      component: Home,
      meta: { requiresAuth: true },
    },
    ...withPrefix('/management', [
      {
        path: '/users',
        name: 'user-management',
        component: UserManagement,
        meta: { requiresAuth: true, requiresUserPerms: true },
      },
      {
        path: '/new-book',
        component: NewBook,
        meta: { requiresAuth: true, requiresBookPerms: true },
        children: [
          {
            path: '/',
            name: 'book-upload',
            component: BookUploader,
          },
          {
            path: ':id/metadata',
            name: 'book-metadata',
            component: MetadataEditor,
          },
          {
            path: ':id/table-of-contents',
            name: 'table-of-contents',
            component: TableOfContents,
          },
          {
            path: ':id/classes',
            name: 'book-classes',
            component: ClassMapper,
          },
        ],
      },
    ]),
    {
      path: '/account',
      name: 'account',
      component: UserProfile,
      meta: { requiresAuth: true },
    },
    {
      path: '/search',
      name: 'search',
      component: SearchResults,
      meta: { requiresAuth: true },
    },
    {
      path: '/chapter/:id',
      name: 'chapter',
      component: Chapter,
      meta: { requiresAuth: true },
    },
  ],
});

router.beforeEach(async (to, from, next) => {
  let auth = store.getters.authorized;
  if (!auth) {
    auth = await store.dispatch('checkIdentity');
  }
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!auth) {
      next(
        {
          path: '/login',
          query: { redirect: to.fullPath },
        },
      );
    } else {
      next();
    }
  } else if (to.matched.some(record => record.meta.requiresUnauthorized)) {
    if (auth) {
      next(from.name === null ? '/' : false);
    } else {
      next();
    }
  } else {
    next();
  }
});

router.beforeEach((to, from, next) => {
  const { identity } = store.state;
  if (identity === null) {
    next();
    return;
  }

  const { canManageBooks, canManageUsers } = identity;
  if (to.matched.some(record => record.meta.requiresBookPerms)) {
    if (!canManageBooks) {
      next(false);
    } else {
      next();
    }
  } else if (to.matched.some(record => record.meta.requiresUserPerms)) {
    if (!canManageUsers) {
      next(false);
    } else {
      next();
    }
  } else {
    next(); // make sure to always call next()!
  }
});

export default router;
