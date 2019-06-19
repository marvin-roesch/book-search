<template>
<div class="user-panel">
  <div class="user-panel-menu-trigger">
    <UserIcon width="28" height="28"></UserIcon>
  </div>
  <ul class="user-panel-links">
    <li>
      <router-link :to="{name: 'home'}">Search</router-link>
    </li>
    <li>
      <router-link :to="{name: 'book-upload'}" v-if="identity.canManageBooks">Books</router-link>
    </li>
    <li>
      <router-link :to="{name: 'user-management'}" v-if="identity.canManageUsers">Users</router-link>
    </li>
  </ul>
</div>
</template>

<script>
import { mapState } from 'vuex';
import { UserIcon } from 'vue-feather-icons';

export default {
  name: 'UserPanel',
  components: { UserIcon },
  computed: mapState(['identity']),
};
</script>

<style lang="scss">
.user-panel {
  display: flex;
  align-items: center;
  padding: 1rem 2rem 1rem 1rem;

  &-menu-trigger {
    width: 28px;
    height: 28px;
    border-radius: 100%;
    border: 2px solid $base-text-color;
  }

  &-links {
    list-style-type: none;
    padding: 0;
    margin: 0 0 0 0.75rem;
    display: flex;
    align-items: center;

    li {
      margin-right: 0.25rem;

      &:after {
        display: inline-block;
        content: '·';
        margin-left: 0.25rem;
      }

      &:last-child {
        margin-right: 0;

        &:after {
          display: none;
        }
      }
    }
  }
}
</style>
