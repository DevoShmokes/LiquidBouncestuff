<script lang="ts">
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import {slide} from "svelte/transition";
    import type {ListSetting, ModuleSetting, RegistryItem} from "../../../../integration/types";
    import VirtualList from "../list/VirtualList.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../theme/theme_config";
    import ExpandArrow from "../common/ExpandArrow.svelte";
    import {setItem} from "../../../../integration/persistent_storage";
    import ListItem from "./ListItem.svelte";
    import {getRegistryItems} from "../../../../integration/rest";
    import {REST_BASE} from "../../../../integration/host";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as ListSetting;
    const thisPath = `${path}.${cSetting.name}`;

    const dispatch = createEventDispatcher();
    let items: TItem[] = [];
    let renderedItems: TItem[] = items;
    let searchQuery = "";
    let expanded = localStorage.getItem(thisPath) === "true";

    interface TItem {
        identifier: string;
        name: string;
    }

    $: setItem(thisPath, expanded.toString());

    $: {
        let filteredItems = items;
        if (searchQuery) {
            filteredItems = filteredItems.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }
        renderedItems = filteredItems;
    }

    onMount(async () => {
        let registryName;
        console.log(cSetting.innerValueType);
        switch (cSetting.innerValueType) {
            case "BLOCK":
                registryName = "blocks";
                break;
            case "ITEM":
                registryName = "items";
                break;
            default:
                console.warn(`Unknown inner value type: ${cSetting.innerValueType}`);
                return;
        }

        const registryItems: Record<string, RegistryItem> = await getRegistryItems(registryName);
        items = Object.entries(registryItems)
            .map(([identifier, item]) => ({
                identifier,
                name: item.name,
                icon: `${REST_BASE}/api/v1/client/resource/itemTexture?id=${identifier}`
            })) as TItem[];
        items = items.sort((a, b) => a.identifier.localeCompare(b.identifier));
    });

    function handleItemToggle(e: CustomEvent<{ identifier: string, enabled: boolean }>) {
        if (e.detail.enabled) {
            cSetting.value = [...cSetting.value, e.detail.identifier];
        } else {
            cSetting.value = cSetting.value.filter(b => b !== e.detail.identifier);
        }

        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="head" class:expanded on:contextmenu|preventDefault={() => expanded = !expanded}>
        <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <ExpandArrow bind:expanded/>
    </div>
    {#if expanded}
        <div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}}>
            <input type="text" placeholder="Search" class="search-input" bind:value={searchQuery} spellcheck="false">
            <div class="results">
                <VirtualList items={renderedItems} let:item>
                    <ListItem identifier={item.identifier} name={item.name} icon={item.icon}
                            enabled={cSetting.value.includes(item.identifier)} on:toggle={handleItemToggle}/>
                </VirtualList>
            </div>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
  }

  .head {
    display: flex;
    justify-content: space-between;
    transition: ease margin-bottom .2s;

    &.expanded {
      margin-bottom: 10px;
    }

    .name {
      color: $clickgui-text-color;
      font-size: 12px;
      font-weight: 500;
    }
  }

  .results {
    height: 200px;
    overflow-y: auto;
    overflow-x: hidden;
    min-height: 100px;
    max-height: 500px;
    position: relative;
  }

  .search-input {
    width: 100%;
    border: none;
    border-bottom: solid 1px $accent-color;
    font-family: "Inter", sans-serif;
    font-size: 12px;
    padding: 5px;
    color: $clickgui-text-color;
    margin-bottom: 5px;
    background-color: rgba($clickgui-base-color, .36);
  }
</style>
