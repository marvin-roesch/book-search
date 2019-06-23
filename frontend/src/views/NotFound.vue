<template>
<Fullscreen user-panel>
  <Card class="not-found">
    <XCircleIcon></XCircleIcon>
    The requested page could not be found!
  </Card>
</Fullscreen>
</template>

<script>
import { XCircleIcon } from 'vue-feather-icons';
import Fullscreen from '@/views/Fullscreen.vue';
import Card from '@/components/Card.vue';

export default {
  name: 'not-found',
  components: { Card, XCircleIcon, Fullscreen },
};
</script>

<style lang="scss">
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 1rem;
  color: $base-text-color;
  margin: 0 auto;
  max-width: 50%;
  text-align: left;

  .feather {
    width: 4rem;
    height: 4rem;
    color: $errors;
    margin-bottom: 0.5rem;
  }
}
</style>
