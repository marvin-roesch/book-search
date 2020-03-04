<template>
<div class="table-of-contents">
  <h2>Select the chapters you want to make searchable</h2>
  <TableOfContentsEntry
    :entries="toc"
    class="table-of-contents-root"
    @change="updateTableOfContents">
  </TableOfContentsEntry>
  <div class="button-bar">
    <Button slim @click="$router.back()" :disabled="updating">Back</Button>
    <Button slim @click="submit" :loading="updating" :disabled="updating">Next</Button>
  </div>
</div>
</template>

<script>
import TableOfContentsEntry from '@/components/management/wizard/TableOfContentsEntry.vue';
import Button from '@/components/Button.vue';

export default {
  name: 'TableOfContents',
  components: { Button, TableOfContentsEntry },
  async mounted() {
    this.updating = true;
    const { id } = this.$route.params;
    if (!id) {
      this.$router.replace({ name: 'book-management' });
      return;
    }

    try {
      const { data: { toc } } = await this.$api.get(`/books/${id}/table-of-contents`);

      this.bookId = id;
      this.toc = toc;
    } catch (error) {
      this.$handleApiError(error);
    }

    this.updating = false;
  },
  data() {
    return {
      bookId: '',
      toc: [],
      updating: false,
    };
  },
  methods: {
    updateTableOfContents({ id, selected }) {
      this.updateTableOfContentsEntry(this.toc, id.split('/'), selected);
    },
    updateTableOfContentsEntry(toc, path, selected) {
      const [head, ...tail] = path;
      const entry = toc.filter(e => e.id === head)[0];
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
        const { data: { showCitations } } = await this.$api.put(
          `/books/${this.bookId}/chapters`,
          entries,
          {
            headers: {
              'Content-Type': 'application/json',
            },
          },
        );
        const route = showCitations ? 'chapter-citations' : 'book-classes';
        this.$router.push({ name: route, params: { id: this.bookId } });
      } catch (error) {
        this.$handleApiError(error);
        this.updating = false;
      }
    },
    linearizeSelectedEntries(toc) {
      return toc
        .flatMap(entry => [
          entry.selected
            ? entry.id
            : undefined,
          ...this.linearizeSelectedEntries(entry.children),
        ])
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
  min-height: 0;

  &-root {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
  }
}
</style>
