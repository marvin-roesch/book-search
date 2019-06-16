import Vue from 'vue';
import Router from 'vue-router';
import Home from './views/Home.vue';
import NewBook from '@/views/NewBook.vue';
import BookUploader from '@/components/wizard/BookUploader.vue';
import TableOfContents from '@/components/wizard/TableOfContents.vue';
import MetadataEditor from '@/components/wizard/MetadataEditor.vue';
import ClassMapper from '@/components/wizard/ClassMapper.vue';
import SearchResults from '@/views/SearchResults.vue';

Vue.use(Router);

export default new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home,
    },
    {
      path: '/new-book',
      name: 'new-book',
      component: NewBook,
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
    {
      path: '/search',
      name: 'search',
      component: SearchResults,
    },
  ],
});
