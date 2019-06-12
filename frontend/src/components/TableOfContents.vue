<template>
<ul class="table-of-contents">
  <li v-for="entry in entries" :key="entry.id">
    <CheckBox :name="entry.id" :value="entry.selected" @input="onDirectChange">{{ entry.title }}</CheckBox>
    <TableOfContents v-if="entry.children.length > 0" :entries="entry.children" @change="onChildChange(entry.id, $event)"></TableOfContents>
  </li>
</ul>
</template>

<script>
import CheckBox from '@/components/CheckBox.vue';

export default {
  name: 'TableOfContents',
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
.table-of-contents {
  list-style-type: none;
  margin: 0;
  padding: 0;

  li {
    padding: 0.25rem 0;

    & > .table-of-contents {
      padding-left: 2rem;
    }
  }
}
</style>
