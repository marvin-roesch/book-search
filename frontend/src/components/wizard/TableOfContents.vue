<template>
<div class="table-of-contents">
  <h2>Select the chapters you want to make searchable</h2>
  <TableOfContentsEntry :entries="toc" class="table-of-contents-root" @change="updateTableOfContents"></TableOfContentsEntry>
  <div class="button-bar">
    <Button slim @click="$router.back()" :disabled="updating">Back</Button>
    <Button slim @click="submit" :loading="updating" :disabled="updating">Next</Button>
  </div>
</div>
</template>

<script>
import TableOfContentsEntry from '@/components/wizard/TableOfContentsEntry.vue';
import Button from '@/components/Button.vue';
import axios from 'axios';

export default {
  name: 'TableOfContents',
  components: { Button, TableOfContentsEntry },
  mounted() {
    const { book } = this.$route.params;
    if (!book) {
      this.$router.replace({ name: 'book-upload' });
      return;
    }
    this.bookId = book.id;
    this.toc = this.mapTableOfContents(book.toc);
  },
  data() {
    return {
      bookId: '',
      toc: [],
      updating: false,
    };
  },
  methods: {
    mapTableOfContents(toc) {
      return toc.map(entry => ({ ...entry, selected: true, children: this.mapTableOfContents(entry.children) }));
    },
    updateTableOfContents({ id, selected }) {
      this.updateTableOfContentsEntry(this.toc, id.split('/'), selected);
    },
    updateTableOfContentsEntry(toc, path, selected) {
      const [head, ...tail] = path;
      const entry = toc.filter(entry => entry.id === head)[0];
      if (tail.length === 0) {
        entry.selected = selected;
      } else {
        this.updateTableOfContentsEntry(entry.children, tail, selected);
      }
    },
    async submit() {
      const entries = this.linearizeSelectedEntries(this.toc);
      this.updating = true;
      try {
        const { data: book } = await axios.put(
          `/api/book/${this.bookId}/table-of-contents`,
          entries,
          {
            headers: {
              'Content-Type': 'application/json',
            },
          },
        );
        setTimeout(
          () => {
            this.$router.push({ name: 'book-classes', params: { book } });
          },
          500,
        );
      } catch (error) {
        this.updating = false;
      }
    },
    linearizeSelectedEntries(toc) {
      return toc
        .flatMap(entry => [entry.selected ? entry.id : undefined, ...this.linearizeSelectedEntries(entry.children)])
        .filter(id => id !== undefined);
    },
  },
};
</script>

<style lang="scss">
.table-of-contents {
  display: flex;
  flex-direction: column;
  align-items: stretch;

  &-root {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
  }
}
</style>
