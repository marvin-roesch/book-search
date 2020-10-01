<template>
  <div class="book-dictionary">
    <LoadingSpinner v-if="loading"></LoadingSpinner>
    <ul v-if="!loading" class="book-dictionary__exports">
      <li>Export as</li>
      <li>
        <a class="book-dictionary__exports-action" href="#" @click.prevent="exportCsv">CSV</a>
      </li>
      <li>
        <a class="book-dictionary__exports-action" href="#" @click.prevent="exportJson">JSON</a>
      </li>
    </ul>
    <transition-group
      class="book-dictionary-entries"
      tag="table"
      name="fade-slide-up"
    >
      <tr :key="'header'">
        <th
          class="book-dictionary-entries-index book-dictionary-entries-header"
          v-if="dictionary.length > 0"
        >
          #
        </th>
        <th
          class="book-dictionary-entries-term book-dictionary-entries-header"
          v-if="dictionary.length > 0"
        >
          Term
        </th>
        <th
          class="book-dictionary-entries-occurrences book-dictionary-entries-header"
          v-if="dictionary.length > 0"
        >
          Occurrences
        </th>
      </tr>
      <template v-for="(entry, index) in dictionary">
      <tr :key="entry.term">
        <td
          class="book-dictionary-entries-index"
          :style="{'transition-delay': `${calculateDelay(index)}ms`}"
        >
          {{ index + 1 }}
        </td>
        <td
          class="book-dictionary-entries-term"
          :style="{'transition-delay': `${calculateDelay(index)}ms`}"
        >
          <router-link
            :to="{
        name: 'search',
        query: { q: entry.term, series: '', books: $route.params.id }
      }"
          >
            {{ entry.term }}
          </router-link>
        </td>
        <td
          class="book-dictionary-entries-occurrences"
          :style="{'transition-delay': `${calculateDelay(index)}ms`}"
        >
          {{ entry.occurrences }}
        </td>
      </tr>
      </template>
    </transition-group>
  </div>
</template>

<script>
import LoadingSpinner from '@/components/search/LoadingSpinner.vue';
import { saveAs } from '@/utils';

export default {
  name: 'book-dictionary',
  components: { LoadingSpinner },
  data() {
    return {
      dictionary: [],
      loading: false,
    };
  },
  async mounted() {
    this.loading = true;
    try {
      const { data: dictionary } = await this.$api.get(`/books/${this.$route.params.id}/dictionary`);
      this.dictionary = dictionary;
    } catch (error) {
      this.$handleApiError(error);
    }
    this.loading = false;
  },
  methods: {
    calculateDelay(index) {
      return Math.min(index * 10, 2000);
    },
    exportCsv() {
      const dict = this.dictionary.map(
        (entry, index) => `${index + 1},${entry.term},${entry.occurrences}`,
      );

      dict.unshift('Rank,Term,Occurrences');

      saveAs(
        new Blob([dict.join('\n')], { type: 'text/csv' }),
        'dictionary.csv',
      );
    },
    exportJson() {
      const dict = this.dictionary.map((entry, index) => ({ rank: index + 1, ...entry }));

      saveAs(
        new Blob([JSON.stringify(dict, undefined, 4)], { type: 'application/json' }),
        'dictionary.json',
      );
    },
  },
};
</script>

<style lang="scss">
.book-dictionary {
  box-sizing: border-box;
  padding: 1rem;

  &__exports {
    display: flex;
    padding: 0;
    list-style-type: none;

    &-action {
      margin-left: 0.5rem;
      color: $primary;
      text-decoration: none;
      transition: color 0.2s ease-in-out;
      cursor: pointer;

      &:hover, &:active, &:focus {
        color: saturate($primary, 10%);
      }
    }

    li {
      display: flex;
      align-items: center;

      &:after {
        content: '●';
        margin-left: 0.5rem;
        cursor: default;
        color: inherit;
        font-size: 0.8em;
      }

      &:first-child:after, &:last-child:after {
        display: none;
      }
    }
  }

  &-entries {
    width: 100%;
    border-collapse: collapse;
    border-spacing: 0;

    th {
      text-align: left;
    }

    &-header {
      font-weight: bold;
      border-bottom: 2px solid rgba(0, 0, 0, 0.2);
      white-space: nowrap
    }

    &-index {
      box-sizing: border-box;
      text-align: right;
      padding: 0.25rem 0.5rem;
      border-right: 1px solid rgba(0, 0, 0, 0.2);
      white-space: nowrap
    }

    &-term {
      box-sizing: border-box;
      padding: 0.25rem 0.5rem;
      width: 50%;
    }

    &-occurrences {
      box-sizing: border-box;
      padding: 0.25rem 0.5rem;
      width: 50%;
    }
  }
}
</style>
