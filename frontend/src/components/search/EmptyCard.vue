<template>
<Card class="empty-card">
  Your search did not have any results.
</Card>
</template>

<script>
import Card from '@/components/Card.vue';

export default {
  name: 'EmptyCard',
  components: { Card },
};
</script>

<style lang="scss">
.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 1rem;
  color: $base-text-color;
  text-align: left;
}
</style>
