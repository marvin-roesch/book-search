<template>
<Card tag="form" :title="title">
  <slot></slot>
  <template slot="footer">
    <slot name="footer"></slot>
  </template>
</Card>
</template>

<script>
import Card from '@/components/Card.vue';

export default {
  name: 'Form',
  components: { Card },
  props: {
    title: String,
  },
};
</script>
