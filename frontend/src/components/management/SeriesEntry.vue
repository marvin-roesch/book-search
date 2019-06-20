<template>
<ul class="series-entry">
  <li v-for="s in series" :key="s.name">
    <span class="series-entry-header">{{ s.name }}</span>
    <ul class="series-entry-books">
      <li v-for="book in s.books" :key="book.id">
        <span class="series-entry-book-title">
          <AlertTriangleIcon
            class="series-entry-book-unsearchable"
            :width="20" :height="20"
            v-if="!book.searchable">
          </AlertTriangleIcon>
          {{ book.title }}
        </span>
        <div class="series-entry-book-actions">
          <router-link
            class="series-entry-book-action"
            :to="{name: 'book-metadata', params: {id: book.id}}">
            <Edit2Icon :width="20" :height="20"></Edit2Icon>
            Edit
          </router-link>
          <a href="#" class="series-entry-book-action" @click.prevent="deleteBook(book.id)">
            <XIcon :width="20" :height="20"></XIcon>
            Delete
          </a>
        </div>
      </li>
    </ul>
    <SeriesEntry :series="s.children" @book-deleted="$emit('book-deleted')"></SeriesEntry>
  </li>
</ul>
</template>

<script>
import { AlertTriangleIcon, Edit2Icon, XIcon } from 'vue-feather-icons';

export default {
  name: 'SeriesEntry',
  components: { AlertTriangleIcon, XIcon, Edit2Icon },
  props: {
    series: Array,
  },
  methods: {
    async deleteBook(bookId) {
      try {
        const { data: { message } } = await this.$api.delete(`/books/${bookId}`);
        this.$emit('book-deleted');
      } catch (error) {
      }
    },
  },
};
</script>

<style lang="scss">
.series-entry {
  list-style-type: none;
  margin: 0;
  padding: 0;
  box-sizing: border-box;

  & > li {
    padding-bottom: 0.5rem;

    &:last-child {
      padding-bottom: 0;
    }
  }

  &-header {
    display: block;
    font-weight: bold;
    padding-bottom: 0.5rem;
  }

  &-books {
    list-style-type: none;
    padding: 0;
    margin: 0;

    li {
      box-sizing: border-box;
      display: flex;
      padding: 0.25rem 0;
      align-items: center;

      &:first-child {
        padding-top: 0;
      }
    }
  }

  &-book {
    &-title {
      display: flex;
      align-items: center;
    }

    &-unsearchable {
      margin-right: 0.25rem;
      color: $errors;
    }

    &-actions {
      margin-left: auto;
      display: flex;
      align-items: stretch;
    }

    &-action {
      color: $base-text-color;
      display: flex;
      align-items: center;
      background: rgba(0, 0, 0, 0.05);
      padding: 0.5rem;
      border-right: 1px solid rgba(0, 0, 0, 0.1);

      &:hover, &:active, &:focus {
        color: $base-text-color;
        background: rgba(0, 0, 0, 0.1);
      }

      .feather {
        margin-right: 0.25rem;
      }

      &:first-child {
        border-top-left-radius: 3px;
        border-bottom-left-radius: 3px;
      }

      &:last-child {
        border-top-right-radius: 3px;
        border-bottom-right-radius: 3px;
        border-right: none;
      }
    }
  }
}
</style>
