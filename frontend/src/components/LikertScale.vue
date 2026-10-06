<template>
    <div class="likert">
        <label v-for="n in 5" :key="n" class="likert-item">
            <input type="radio" :name="name" :value="n" v-model="model" />
            <span>{{ n }}</span>
        </label>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ name: string; modelValue?: number }>();
const emit = defineEmits(['update:modelValue']);
const model = computed({
    get: () => props.modelValue ?? 0,
    set: (v: number) => emit('update:modelValue', Number(v))
});
</script>

<style scoped>
.likert {
    display: flex;
    gap: .75rem;
}

.likert-item {
    display: flex;
    flex-direction: column;
    align-items: center;
}
</style>
