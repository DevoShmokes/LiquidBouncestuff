<script lang="ts">
    import {onMount} from "svelte";
    import {listen} from "../../../integration/ws";
    import {getClientUser, loginClientUser, logoutClientUser} from "../../../integration/rest";
    import type {ClientUser} from "../../../integration/types";

    let user: ClientUser | null = null;
    let loading = true;

    async function refreshUser() {
        loading = true;
        try {
            user = await getClientUser();
        } catch (e) {
            console.error("Failed to get client user:", e);
            user = null;
        } finally {
            loading = false;
        }
    }

    async function login() {
        try {
            await loginClientUser();
        } catch (e) {
            console.error("Login failed:", e);
            loading = false;
        }
    }

    async function logout() {
        try {
            await logoutClientUser();
        } catch (e) {
            console.error("Logout failed:", e);
            loading = false;
        }
    }

    onMount(async () => {
        try {
            await refreshUser();
        } catch (e) {
            loading = false;
        }
    });

    listen("userLoggedIn", refreshUser);
    listen("userLoggedOut", () => {
        user = null;
        loading = false;
    });

    onMount(() => {
        const timeout = setTimeout(() => {
            if (loading) loading = false;
        }, 3000);
        return () => clearTimeout(timeout);
    });

    $: avatarUrl = user?.nickname 
        ? `https://avatar.liquidbounce.net/avatar/${user.nickname}` 
        : "img/steve.png";
    
    function formatGroups(groups: string[]): string {
        if (!groups || groups.length === 0) return "User";
        if (groups.length === 1) return groups[0];
        return `${groups[0]} +${groups.length - 1}`;
    }
</script>

<div class="user-box">
    {#if !loading}
        {#if user}
            <div class="user-info">
                <div class="avatar-container">
                    <img src={avatarUrl} alt="avatar" class="avatar">
                    {#if user.premium}
                        <div class="premium-badge">
                            <img src="img/menu/icon-star.svg" alt="premium" class="star-icon">
                        </div>
                    {/if}
                </div>
                <div class="user-details">
                    <div class="username">{user.nickname || user.name || "Unknown"}</div>
                    <div class="groups" title={user.groups.join(", ")}>
                        {formatGroups(user.groups)}
                    </div>
                </div>
                <button class="logout-button" on:click={logout} title="Logout">
                    <img src="img/menu/icon-logout.svg" alt="logout" class="logout-icon">
                </button>
            </div>
        {:else}
            <div class="login-prompt">
                <div class="avatar-container">
                    <img src="img/steve.png" alt="avatar" class="avatar steve-avatar">
                </div>
                <div class="login-text">Sign in to LiquidBounce</div>
                <button class="login-button" on:click={login}>
                    <img src="img/menu/icon-login.svg" alt="login" class="login-icon">
                    Login
                </button>
            </div>
        {/if}
    {:else}
        <div class="loading">Loading...</div>
    {/if}
</div>

<style lang="scss">
    @use "../../../colors.scss" as *;

    .user-box {
        background-color: rgba($menu-base-color, 0.68);
        width: 590px;
        padding: 25px 35px;
        border-radius: 5px;
        display: flex;
        align-items: center;
        margin-bottom: 25px;
        transition: background-color 0.2s ease-out;
        cursor: pointer;

        &:hover {
            background-color: rgba(lighten($menu-base-color, 5%), 0.68);
        }
    }

    .loading {
        color: $menu-text-dimmed-color;
        text-align: center;
        font-size: 16px;
        padding: 15px;
        width: 100%;
    }

    .user-info {
        display: flex;
        align-items: center;
        gap: 25px;
        width: 100%;
    }

    .avatar-container {
        position: relative;
        flex-shrink: 0;
        width: 90px;
        height: 90px;
        border-radius: 50%;
        background-color: $accent-color;
        overflow: hidden;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .avatar {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 50%;
    }

    .steve-avatar {
        width: 100%;
        height: 100%;
        object-fit: contain;
        image-rendering: pixelated;
    }

    .premium-badge {
        position: absolute;
        top: 3px;
        right: -2px;
        width: 34px;
        height: 34px;
        display: flex;
        align-items: center;
        justify-content: center;

        .star-icon {
            width: 30px;
            height: 30px;
            filter: drop-shadow(0px 0px 2px rgba(0, 0, 0, 0.5));
        }
    }

    .user-details {
        flex: 1;
        min-width: 0;
        overflow: hidden;
    }

    .username {
        font-weight: 600;
        color: $menu-text-color;
        font-size: 26px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        margin-bottom: 5px;
    }

    .groups {
        font-size: 18px;
        color: $menu-text-dimmed-color;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .logout-button {
        background: none;
        border: none;
        cursor: pointer;
        padding: 8px;
        color: $menu-text-dimmed-color;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: color 0.2s;
        flex-shrink: 0;

        &:hover {
            color: $menu-text-color;
        }

        .logout-icon {
            width: 28px;
            height: 28px;
            filter: invert(70%);
            
            &:hover {
                filter: invert(100%);
            }
        }
    }

    .login-prompt {
        display: flex;
        align-items: center;
        gap: 25px;
        width: 100%;
    }

    .login-text {
        flex: 1;
        font-size: 26px;
        color: $menu-text-color;
        font-weight: 600;
    }

    .login-button {
        background-color: $accent-color;
        color: white;
        border: none;
        border-radius: 3px;
        padding: 10px 24px;
        font-size: 16px;
        cursor: pointer;
        transition: background-color 0.2s;
        display: flex;
        align-items: center;
        gap: 8px;

        &:hover {
            background-color: darken($accent-color, 10%);
        }
        
        .login-icon {
            width: 20px;
            height: 20px;
            filter: brightness(0) invert(1);
        }
    }
</style>
