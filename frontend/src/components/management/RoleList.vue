<template>
<Card class="role-list">
  <template slot="title">
  <template v-if="hasPermission('books.manage')">
  <router-link :to="{name: 'book-management'}">Books</router-link>
  <span>&middot;</span>
  </template>
  <router-link :to="{name: 'user-management'}">Users</router-link>
  <span>&middot;</span>
  <span>Roles</span>
  </template>
  <div class="role-table">
    <div class="role-table-header">Name</div>
    <div class="role-table-header">Permissions</div>
    <div class="role-table-header">Actions</div>
    <template v-for="role in roles">
    <div class="role-table-cell" :key="`${role.id}-name`">
      {{ role.name }}
    </div>
    <div class="role-table-cell" :key="`${role.id}-permissions`">
      <multiselect
        v-model="role.selectedPermissions"
        :options="permissions"
        :multiple="true"
        placeholder="Select permissions"
        label="description"
        track-by="id"
        :close-on-select="false"
        :clear-on-select="false"
        :limit="5"
        @close="updateRole(role)"
      >
        <template slot="tag" slot-scope="{ option }">
        <span class="multiselect__tag multiselect__tag-no-icon" :key="option.id">
          <span>{{ option.description }}</span>
        </span>
        </template>
      </multiselect>
    </div>
    <div class="role-table-cell" :key="`${role.id}-actions`">
      <a href="#" @click.prevent="deleteRole(role.id)">Delete</a>
    </div>
    </template>
  </div>
  <form class="role-list-form">
    <h3>Create new role</h3>
    <TextField name="new-role-name" placeholder="Name" v-model="newName">
      <template slot="icon">
      <TypeIcon></TypeIcon>
      </template>
    </TextField>
    <multiselect
      v-model="newPermissions"
      :options="permissions"
      :multiple="true"
      placeholder="Select permissions"
      label="description"
      track-by="id"
      :close-on-select="false"
      :clear-on-select="false"
      :limit="5"
    >
    </multiselect>
    <div class="card-footer">
      <a href="#" @click.prevent="$router.back()">Back</a>
      <Button
        slim
        :disabled="creating || newName.length === 0"
        @click="createRole"
      >
        Create
      </Button>
    </div>
  </form>
</Card>
</template>

<script>
import { mapGetters, mapState } from 'vuex';
import { TypeIcon } from 'vue-feather-icons';
import Multiselect from 'vue-multiselect';
import Card from '@/components/Card.vue';
import TextField from '@/components/TextField.vue';
import Button from '@/components/Button.vue';

export default {
  name: 'RoleList',
  components: { Button, TypeIcon, TextField, Card, Multiselect },
  data() {
    return {
      roles: [],
      permissions: [],
      newName: '',
      newPermissions: [],
      creating: false,
    };
  },
  computed: { ...mapState('auth', ['identity']), ...mapGetters('auth', ['hasPermission']) },
  async mounted() {
    try {
      const { data: roles } = await this.$api.get('/auth/roles');
      const { data: permissions } = await this.$api.get('/auth/permissions');
      this.roles = roles.map(r => ({
        ...r,
        selectedPermissions: permissions.filter(p => r.permissions.includes(p.id)),
      }));
      this.permissions = permissions;
    } catch (error) {
      this.$handleApiError(error);
    }
  },
  methods: {
    async updateRole(role) {
      try {
        const { data: { message } } = await this.$api.patch(
          `/auth/roles/${role.id}`,
          {
            permissions: role.selectedPermissions.map(p => p.id),
          },
        );
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
    },
    async deleteRole(roleId) {
      try {
        const { data: { message } } = await this.$api.delete(`/auth/roles/${roleId}`);
        this.roles = this.roles.filter(r => r.id !== roleId);
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
    },
    async createRole() {
      this.creating = true;
      try {
        const { data: { message, role } } = await this.$api.put(
          '/auth/roles',
          {
            name: this.newName,
            initialPermissions: this.newPermissions.map(p => p.id),
          },
        );
        this.roles.push({
          ...role,
          selectedPermissions: this.permissions.filter(p => role.permissions.includes(p.id)),
        });
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
      this.creating = false;
    },
  },
};
</script>

<style scoped lang="scss">
.role-list {
  margin: 0 auto;
  width: 50vw;
  max-width: $max-content-width;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  box-sizing: border-box;

  h2 {
    display: flex;
    align-items: center;

    span {
      margin-left: 0.25rem;
    }

    a {
      margin-left: 0.25rem;
    }
  }

  .role-table {
    padding: 0;
    margin: 0;
    width: 100%;
    border-collapse: collapse;
    border-spacing: 0;
    display: grid;
    grid-template-columns: auto 1fr auto;
    overflow-y: auto;
    max-height: 100%;
    border-bottom: 1px solid rgba(0, 0, 0, 0.2);
    position: relative;
    align-items: center;

    &-header, &-cell {
      padding: 0.5rem;
    }

    &-header {
      position: sticky;
      font-weight: bold;
      border-bottom: 1px solid rgba(0, 0, 0, 0.2);
      top: 0;
      background: var(--section-bg);
      z-index: 1000;
    }

    &-cell {
      &:first-child {
        width: 100%;
      }

      &:nth-child(2), &:last-child {
        white-space: nowrap
      }

      a {
        margin-right: 0.5rem;

        &:last-child {
          margin-right: 0;
        }
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

  &-form {
    padding-top: 1rem;

    h3 {
      padding: 0;
      margin: 0;
    }

    .checkbox {
      display: inline-block;
      margin-top: 0.5rem;
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }

    .button {
      margin-top: 0.5rem;
      align-self: flex-end;
    }
  }

  @media (max-width: $max-content-width) {
    width: 100%;
  }
}
</style>
