<template>
<Card class="user-list" title="Users">
  <table class="user-table">
    <thead>
    <tr>
      <th>Username</th>
      <th>Permissions</th>
      <th>Actions</th>
    </tr>
    </thead>
    <tbody>
    <tr v-for="user in users" :key="user.id">
      <td>
        {{ user.username }}
      </td>
      <td>
        <CheckBox :name="`${user.id}-manage-books`" :value="user.canManageBooks">
          Manage books
        </CheckBox>
        <CheckBox :name="`${user.id}-manage-users`" :value="user.canManageUsers">
          Manage users
        </CheckBox>
      </td>
      <td>
        <a href="#" @click.prevent="deleteUser(user.id)">Delete</a>
      </td>
    </tr>
    </tbody>
  </table>
</Card>
</template>

<script>
import Card from '@/components/Card.vue';
import CheckBox from '@/components/CheckBox.vue';

export default {
  name: 'UserList',
  components: { CheckBox, Card },
  data() {
    return {
      users: [],
    };
  },
  async mounted() {
    try {
      const { data: users } = await this.$api.get('/auth/users');
      this.users = users;
    } catch (error) {
    }
  },
  methods: {
    async deleteUser(userId) {

    },
  },
};
</script>

<style scoped lang="scss">
.user-list {
  margin: 0 auto;
  width: 30vw;
  max-height: 80vh;

  .user-table {
    padding: 0;
    margin: 0;
    width: 100%;
    border-collapse: collapse;
    border-spacing: 0;

    th {
      text-align: left;
      border-bottom: 1px solid rgba(0, 0, 0, 0.2);
      padding: 0.5rem;
    }

    td {
      padding: 0.5rem;

      &:first-child {
        width: 100%;
      }

      &:nth-child(2), &:last-child {
        white-space: nowrap
      }
    }

    .checkbox {
      display: inline-block;
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }
  }
}
</style>
