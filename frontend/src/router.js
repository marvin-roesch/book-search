import Vue from 'vue';
import Router from 'vue-router';
import Home from './views/Home.vue';
import TableOfContents from '@/components/management/wizard/TableOfContents.vue';
import MetadataEditor from '@/components/management/wizard/MetadataEditor.vue';
import ClassMapper from '@/components/management/wizard/ClassMapper.vue';
import SearchResults from '@/views/SearchResults.vue';
import Chapter from '@/views/Chapter.vue';
import store from '@/store';
import Login from '@/views/Login.vue';
import UserManagement from '@/views/UserManagement.vue';
import UserProfile from '@/views/UserProfile.vue';
import BookManagement from '@/views/BookManagement.vue';
import NewBook from '@/views/NewBook.vue';
import EditBook from '@/views/EditBook.vue';
import NotFound from '@/views/NotFound.vue';
import Library from '@/views/Library.vue';
import BookOverview from '@/views/BookOverview.vue';
import BookChapters from '@/views/BookChapters.vue';
import BookDictionary from '@/views/BookDictionary.vue';
import ChapterOverlay from '@/components/search/ChapterOverlay.vue';
import ReuploadBook from '@/components/management/wizard/ReuploadBook.vue';

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
        path: '/books',
        name: 'book-management',
        component: BookManagement,
        meta: { requiresAuth: true, requiresBookPerms: true },
      },
      ...withPrefix('/books', [
        {
          path: '/new',
          name: 'book-upload',
          component: NewBook,
          meta: { requiresAuth: true, requiresBookPerms: true },
        },
        {
          path: '/edit/:id',
          meta: { requiresAuth: true, requiresBookPerms: true },
          component: EditBook,
          children: [
            {
              path: '/',
              name: 'book-metadata',
              component: MetadataEditor,
            },
            {
              path: 'reupload',
              name: 'book-reupload',
              component: ReuploadBook,
              meta: { requiresAuth: true, requiresBookPerms: true },
            },
            {
              path: 'table-of-contents',
              name: 'table-of-contents',
              component: TableOfContents,
            },
            {
              path: 'classes',
              name: 'book-classes',
              component: ClassMapper,
            },
          ],
        },
      ]),
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
      children: [
        {
          path: 'preview/:id',
          name: 'search-preview',
          component: ChapterOverlay,
        },
      ],
    },
    {
      path: '/chapters/:id',
      name: 'chapter',
      component: Chapter,
      meta: { requiresAuth: true },
    },
    {
      path: '/library',
      name: 'library',
      component: Library,
      meta: { requiresAuth: true },
    },
    {
      path: '/library/:id',
      component: BookOverview,
      meta: { requiresAuth: true },
      children: [
        {
          path: '/',
          name: 'book-chapters',
          component: BookChapters,
          meta: { coverTransition: true, title: 'Chapters' },
        },
        {
          path: 'dictionary',
          name: 'book-dictionary',
          component: BookDictionary,
          meta: { coverTransition: true, title: 'Dictionary' },
        },
      ],
    },
    {
      path: '*',
      name: 'not-found',
      component: NotFound,
    },
  ],
  scrollBehavior(to, from) {
    if (to.name === 'chapter') {
      return { x: 0, y: 0 };
    }
    if (from.name === 'library' && to.path.startsWith('/library/')) {
      return { x: 0, y: 0 };
    }
    return undefined;
  },
});

router.beforeEach(async (to, from, next) => {
  let auth = store.getters['auth/authorized'];
  if (!auth) {
    auth = await store.dispatch('auth/checkIdentity');
  }
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!auth) {
      store.dispatch(
        'notifications/push',
        { type: 'error', message: 'You have to be logged in to view this page!' },
      );
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
      store.dispatch(
        'notifications/push',
        { type: 'error', message: 'You must not be logged in to view this page!' },
      );
      next(from.name === null ? '/' : false);
    } else {
      next();
    }
  } else {
    next();
  }
});

router.beforeEach((to, from, next) => {
  const { identity } = store.state.auth;
  if (identity === null) {
    next();
    return;
  }

  const hasPermission = store.getters['auth/hasPermission'];

  if (to.matched.some(record => record.meta.requiresBookPerms)) {
    if (!hasPermission('books.manage')) {
      store.dispatch(
        'notifications/push',
        { type: 'error', message: 'You are not authorized to view the requested page!' },
      );
      next(from.name === null ? '/' : false);
    } else {
      next();
    }
  } else if (to.matched.some(record => record.meta.requiresUserPerms)) {
    if (!hasPermission('users.manage')) {
      store.dispatch(
        'notifications/push',
        { type: 'error', message: 'You are not authorized to view the requested page!' },
      );
      next(from.name === null ? '/' : false);
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
