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
import RoleManagement from '@/views/RoleManagement.vue';
import ChapterCitations from '@/components/management/wizard/ChapterCitations.vue';

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
      meta: { requiresUnauthorized: true, title: 'Login' },
    },
    {
      path: '/',
      name: 'home',
      component: Home,
      meta: { requiresAuth: true, title: 'Home' },
    },
    ...withPrefix('/management', [
      {
        path: '/users',
        name: 'user-management',
        component: UserManagement,
        meta: { requiresAuth: true, requiredPermissions: 'users.manage', title: 'Users' },
      },
      {
        path: '/roles',
        name: 'role-management',
        component: RoleManagement,
        meta: { requiresAuth: true, requiredPermissions: 'users.manage', title: 'Roles' },
      },
      {
        path: '/books',
        name: 'book-management',
        component: BookManagement,
        meta: { requiresAuth: true, requiredPermissions: 'books.manage', title: 'Books' },
      },
      ...withPrefix('/books', [
        {
          path: '/new',
          name: 'book-upload',
          component: NewBook,
          meta: { requiresAuth: true, requiredPermissions: 'books.manage', title: 'Upload Book' },
        },
        {
          path: '/edit/:id',
          meta: { requiresAuth: true, requiredPermissions: 'books.manage', title: 'Edit Book' },
          component: EditBook,
          children: [
            {
              path: '/',
              name: 'book-metadata',
              component: MetadataEditor,
              meta: { requiresAuth: true, requiredPermissions: 'books.manage', title: 'Edit Metadata' },
            },
            {
              path: 'reupload',
              name: 'book-reupload',
              component: ReuploadBook,
              meta: { requiresAuth: true, requiredPermissions: 'books.manage', title: 'Upload New Version' },
            },
            {
              path: 'table-of-contents',
              name: 'table-of-contents',
              component: TableOfContents,
              meta: { requiresAuth: true, requiredPermissions: 'books.manage', title: 'Table of Contents' },
            },
            {
              path: 'citations',
              name: 'chapter-citations',
              component: ChapterCitations,
              meta: { requiresAuth: true, requiredPermissions: 'books.manage', title: 'Chapter Citations' },
            },
            {
              path: 'classes',
              name: 'book-classes',
              component: ClassMapper,
              meta: { requiresAuth: true, requiredPermissions: 'books.manage', title: 'Styles' },
            },
          ],
        },
      ]),
    ]),
    {
      path: '/account',
      name: 'account',
      component: UserProfile,
      meta: { requiresAuth: true, title: 'Account' },
    },
    {
      path: '/search',
      name: 'search',
      component: SearchResults,
      meta: { requiresAuth: true, autoTitle: false },
      children: [
        {
          path: 'preview/:id',
          name: 'search-preview',
          component: ChapterOverlay,
          meta: { autoTitle: false },
        },
      ],
    },
    {
      path: '/chapters/:id',
      name: 'chapter',
      component: Chapter,
      meta: { requiresAuth: true, autoTitle: false },
    },
    {
      path: '/library',
      name: 'library',
      component: Library,
      meta: { requiresAuth: true, title: 'Library' },
    },
    {
      path: '/library/:id',
      component: BookOverview,
      meta: { requiresAuth: true, autoTitle: false },
      children: [
        {
          path: '/',
          name: 'book-chapters',
          component: BookChapters,
          meta: { coverTransition: true, autoTitle: false },
        },
        {
          path: 'dictionary',
          name: 'book-dictionary',
          component: BookDictionary,
          meta: { coverTransition: true, autoTitle: false },
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

  if (to.matched.some(record => record.meta.requiredPermissions)) {
    if (!hasPermission(to.meta.requiredPermissions)) {
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

router.afterEach((to) => {
  if (to.meta.autoTitle === false) {
    return;
  }

  Vue.nextTick(() => {
    document.title = to.meta.title ? `${to.meta.title} · Book Search` : 'Book Search';
  });
});

export default router;
