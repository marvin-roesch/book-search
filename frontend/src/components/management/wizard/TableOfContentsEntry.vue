<template>
<ul class="table-of-contents-entry">
  <li v-for="entry in entries" :key="entry.id">
    <CheckBox :name="entry.id" :value="entry.selected" @input="onDirectChange">
      {{ entry.title }}
    </CheckBox>
    <TableOfContentsEntry v-if="entry.children.length > 0" :entries="entry.children"
                          @change="onChildChange(entry.id, $event)"></TableOfContentsEntry>
  </li>
</ul>
</template>

<script>
import CheckBox from '@/components/CheckBox.vue';

export default {
  name: 'TableOfContentsEntry',
  components: { CheckBox },
  props: {
    entries: Array,
  },
  methods: {
    onDirectChange(event) {
      this.$emit('change', { id: event.target.name, selected: event.target.checked });
    },
    onChildChange(parent, { id, selected }) {
      this.$emit('change', { id: `${parent}/${id}`, selected });
    },
  },
};
</script>

<style lang="scss">
.table-of-contents-entry {
  list-style-type: none;
  margin: 0;
  padding: 0;
  box-sizing: border-box;

  li {
    padding: 0.25rem 0;
    box-sizing: border-box;

    & > .table-of-contents-entry {
      padding-left: 2rem;
    }
  }
}
</style>
