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
      this.$router.replace({ name: 'book-upload' });
      return;
    }

    const { data: { toc } } = await this.$api.get(`/book/${id}/table-of-contents`);

    this.bookId = id;
    this.toc = this.mapTableOfContents(toc);
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
    mapTableOfContents(toc) {
      return toc.map(entry => ({
        ...entry,
        selected: true,
        children: this.mapTableOfContents(entry.children),
      }));
    },
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
        await this.$api.put(
          `/book/${this.bookId}/chapters`,
          entries,
          {
            headers: {
              'Content-Type': 'application/json',
            },
          },
        );
        this.$router.push({ name: 'book-classes', params: { id: this.bookId } });
      } catch (error) {
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

  &-root {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
  }
}
</style>
