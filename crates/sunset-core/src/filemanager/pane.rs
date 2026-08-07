//! 双栏状态机：窗格布局、导航栈（前进/后退/上级）、跨栏传输目标。
//!
//! 移植自 Kotlin `FileManagerDualPaneState`、`FileManagerPaneNavigationState`、
//! `FileManagerDualPaneNavigationState` 与 `FileManagerPaneTransferTarget`，
//! 状态转换语义保持一一对应。

use crate::filemanager::path::{normalize_path, parent_path};

/// 窗格标识。
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum PaneId {
    Left,
    Right,
}

impl PaneId {
    /// 对侧窗格。
    pub fn opposite(self) -> PaneId {
        match self {
            PaneId::Left => PaneId::Right,
            PaneId::Right => PaneId::Left,
        }
    }
}

/// 双栏布局状态（与 Kotlin FileManagerDualPaneState 字段/方法对齐）。
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct DualPaneState {
    pub is_dual_pane: bool,
    pub focused_pane: PaneId,
    pub source_pane: PaneId,
    pub target_pane: PaneId,
}

/// 宽屏阈值（dp），与 Kotlin WideScreenMinDp 一致。
pub const WIDE_SCREEN_MIN_DP: i32 = 600;

impl DualPaneState {
    /// 创建状态；强制保证 source != target。
    pub fn new(is_dual_pane: bool) -> Self {
        Self {
            is_dual_pane,
            focused_pane: PaneId::Left,
            source_pane: PaneId::Left,
            target_pane: PaneId::Right,
        }
    }

    /// 聚焦指定窗格（单栏模式始终聚焦左栏）。
    pub fn focus(self, pane: PaneId) -> Self {
        if self.is_dual_pane {
            Self {
                focused_pane: pane,
                ..self
            }
        } else {
            Self {
                focused_pane: PaneId::Left,
                ..self
            }
        }
    }

    /// 切换焦点到对侧窗格。
    pub fn toggle_focus(self) -> Self {
        self.focus(self.focused_pane.opposite())
    }

    /// 设置源窗格（目标自动为对侧，焦点同步）。
    pub fn with_source_pane(self, pane: PaneId) -> Self {
        if self.is_dual_pane {
            Self {
                source_pane: pane,
                target_pane: pane.opposite(),
                focused_pane: pane,
                ..self
            }
        } else {
            Self {
                source_pane: PaneId::Left,
                target_pane: PaneId::Right,
                focused_pane: PaneId::Left,
                ..self
            }
        }
    }

    /// 交换源/目标窗格。
    pub fn swap_panes(self) -> Self {
        if self.is_dual_pane {
            Self {
                source_pane: self.target_pane,
                target_pane: self.source_pane,
                focused_pane: self.target_pane,
                ..self
            }
        } else {
            self
        }
    }

    /// 根据屏幕配置决定是否启用双栏。
    pub fn from_configuration(
        screen_width_dp: i32,
        is_landscape: bool,
        user_enabled_dual_pane: bool,
        force_dual_pane: bool,
    ) -> Self {
        let is_wide_screen = screen_width_dp >= WIDE_SCREEN_MIN_DP;
        let is_dual_pane =
            user_enabled_dual_pane && (force_dual_pane || (is_landscape && is_wide_screen));
        Self::new(is_dual_pane)
    }
}

/// 单窗格导航状态（与 Kotlin FileManagerPaneNavigationState 对齐）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PaneNavigationState {
    pub pane_id: PaneId,
    pub current_path: String,
    pub back_stack: Vec<String>,
    pub forward_stack: Vec<String>,
}

impl PaneNavigationState {
    /// 创建根状态。
    pub fn root(pane_id: PaneId, path: &str) -> Self {
        Self {
            pane_id,
            current_path: normalize_path(path),
            back_stack: Vec::new(),
            forward_stack: Vec::new(),
        }
    }

    /// 是否可后退。
    pub fn can_go_back(&self) -> bool {
        !self.back_stack.is_empty()
    }

    /// 是否可前进。
    pub fn can_go_forward(&self) -> bool {
        !self.forward_stack.is_empty()
    }

    /// 是否可向上一级（根路径不可）。
    pub fn can_go_up(&self) -> bool {
        !self.current_path.starts_with("content://") && parent_path(&self.current_path).is_some()
    }

    /// 更新窗格标识（用于双栏交换）。
    pub fn with_pane_id(self, pane_id: PaneId) -> Self {
        Self { pane_id, ..self }
    }

    /// 进入新路径：压入后退栈、清空前进栈。
    pub fn enter(self, path: &str) -> Self {
        let normalized = normalize_path(path);
        if normalized == self.current_path {
            return self;
        }
        Self {
            current_path: normalized,
            back_stack: {
                let mut stack = self.back_stack;
                stack.push(self.current_path);
                stack
            },
            forward_stack: Vec::new(),
            ..self
        }
    }

    /// 替换当前路径（重置栈）。
    pub fn replace(self, path: &str) -> Self {
        Self {
            current_path: normalize_path(path),
            back_stack: Vec::new(),
            forward_stack: Vec::new(),
            ..self
        }
    }

    /// 后退一步。
    pub fn go_back(self) -> Self {
        if self.back_stack.is_empty() {
            return self;
        }
        let mut back_stack = self.back_stack;
        let previous = back_stack.pop().expect("checked non-empty");
        let mut forward_stack = self.forward_stack;
        forward_stack.push(self.current_path);
        Self {
            current_path: previous,
            back_stack,
            forward_stack,
            ..self
        }
    }

    /// 前进一步。
    pub fn go_forward(self) -> Self {
        if self.forward_stack.is_empty() {
            return self;
        }
        let mut forward_stack = self.forward_stack;
        let next = forward_stack.pop().expect("checked non-empty");
        let mut back_stack = self.back_stack;
        back_stack.push(self.current_path);
        Self {
            current_path: next,
            back_stack,
            forward_stack,
            ..self
        }
    }

    /// 向上一级。
    pub fn go_up(self) -> Self {
        let Some(parent) = parent_path(&self.current_path) else {
            return self;
        };
        self.enter(&parent)
    }
}

/// 双栏导航状态（与 Kotlin FileManagerDualPaneNavigationState 对齐）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DualPaneNavigationState {
    pub left: PaneNavigationState,
    pub right: PaneNavigationState,
}

impl DualPaneNavigationState {
    /// 双栏根状态。
    pub fn roots(left_path: &str, right_path: &str) -> Self {
        Self {
            left: PaneNavigationState::root(PaneId::Left, left_path),
            right: PaneNavigationState::root(PaneId::Right, right_path),
        }
    }

    /// 获取指定窗格状态。
    pub fn pane(&self, pane_id: PaneId) -> &PaneNavigationState {
        match pane_id {
            PaneId::Left => &self.left,
            PaneId::Right => &self.right,
        }
    }

    /// 更新指定窗格状态。
    pub fn update_pane(
        self,
        pane_id: PaneId,
        update: impl FnOnce(PaneNavigationState) -> PaneNavigationState,
    ) -> Self {
        match pane_id {
            PaneId::Left => Self {
                left: update(self.left),
                ..self
            },
            PaneId::Right => Self {
                right: update(self.right),
                ..self
            },
        }
    }

    /// 指定窗格进入路径。
    pub fn enter(self, pane_id: PaneId, path: &str) -> Self {
        self.update_pane(pane_id, |s| s.enter(path))
    }

    /// 指定窗格替换路径。
    pub fn replace(self, pane_id: PaneId, path: &str) -> Self {
        self.update_pane(pane_id, |s| s.replace(path))
    }

    /// 指定窗格后退。
    pub fn go_back(self, pane_id: PaneId) -> Self {
        self.update_pane(pane_id, |s| s.go_back())
    }

    /// 指定窗格前进。
    pub fn go_forward(self, pane_id: PaneId) -> Self {
        self.update_pane(pane_id, |s| s.go_forward())
    }

    /// 指定窗格向上一级。
    pub fn go_up(self, pane_id: PaneId) -> Self {
        self.update_pane(pane_id, |s| s.go_up())
    }

    /// 交换左右窗格（保留各自导航栈）。
    pub fn swap_panes(self) -> Self {
        Self {
            left: self.right.with_pane_id(PaneId::Left),
            right: self.left.with_pane_id(PaneId::Right),
        }
    }
}

/// 跨栏传输目标（与 Kotlin FileManagerPaneTransferTarget 对齐）。
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PaneTransferTarget {
    pub source_pane: PaneId,
    pub target_pane: PaneId,
    pub source_path: String,
    pub target_path: String,
    pub is_explicit_dual_pane_target: bool,
}

impl PaneTransferTarget {
    /// 源与目标路径是否不同（避免无意义操作）。
    pub fn has_distinct_target_path(&self) -> bool {
        self.source_path != self.target_path
    }
}

/// 传输目标解析器（与 Kotlin FileManagerPaneTransferTargetResolver 对齐）。
pub struct PaneTransferTargetResolver;

impl PaneTransferTargetResolver {
    /// 由双栏布局状态 + 导航状态解析当前传输目标；单栏模式返回 `None`。
    pub fn resolve(
        pane_state: &DualPaneState,
        navigation_state: &DualPaneNavigationState,
    ) -> Option<PaneTransferTarget> {
        if !pane_state.is_dual_pane {
            return None;
        }
        let source_pane = pane_state.focused_pane;
        let target_pane = source_pane.opposite();
        let source_path = navigation_state.pane(source_pane).current_path.clone();
        let target_path = navigation_state.pane(target_pane).current_path.clone();
        Some(PaneTransferTarget {
            source_pane,
            target_pane,
            source_path,
            target_path,
            is_explicit_dual_pane_target: true,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn dual_pane_state_transitions() {
        let state = DualPaneState::new(true);
        assert_eq!(state.focused_pane, PaneId::Left);
        assert_eq!(state.source_pane, PaneId::Left);
        assert_eq!(state.target_pane, PaneId::Right);

        let focused_right = state.focus(PaneId::Right);
        assert_eq!(focused_right.focused_pane, PaneId::Right);
        assert_eq!(focused_right.toggle_focus().focused_pane, PaneId::Left);

        let swapped = focused_right.swap_panes();
        assert_eq!(swapped.source_pane, PaneId::Right);
        assert_eq!(swapped.target_pane, PaneId::Left);

        let with_source = swapped.with_source_pane(PaneId::Left);
        assert_eq!(with_source.source_pane, PaneId::Left);
        assert_eq!(with_source.target_pane, PaneId::Right);
    }

    #[test]
    fn single_pane_ignores_focus() {
        let state = DualPaneState::new(false);
        assert_eq!(state.focus(PaneId::Right).focused_pane, PaneId::Left);
        assert_eq!(state.swap_panes(), state);
    }

    #[test]
    fn from_configuration_rules() {
        assert!(DualPaneState::from_configuration(720, true, true, false).is_dual_pane);
        assert!(!DualPaneState::from_configuration(500, true, true, false).is_dual_pane);
        assert!(!DualPaneState::from_configuration(720, false, true, false).is_dual_pane);
        // 用户禁用时即使 force 也不启用（与 Kotlin 语义一致）
        assert!(!DualPaneState::from_configuration(720, true, false, false).is_dual_pane);
        assert!(!DualPaneState::from_configuration(500, true, false, true).is_dual_pane);
        assert!(DualPaneState::from_configuration(720, true, true, true).is_dual_pane);
    }

    #[test]
    fn navigation_back_forward_up() {
        let state = PaneNavigationState::root(PaneId::Left, "/a");
        assert!(!state.can_go_back());
        assert!(!state.can_go_forward());
        assert!(state.can_go_up());

        let entered = state.enter("/a/b");
        assert_eq!(entered.current_path, "/a/b");
        assert_eq!(entered.back_stack, vec!["/a".to_string()]);
        assert!(entered.can_go_back());
        assert!(!entered.can_go_forward());

        let back = entered.clone().go_back();
        assert_eq!(back.current_path, "/a");
        assert!(back.back_stack.is_empty());
        assert_eq!(back.forward_stack, vec!["/a/b".to_string()]);
        assert!(back.can_go_forward());

        let forward = back.clone().go_forward();
        assert_eq!(forward.current_path, "/a/b");

        let up = entered.go_up();
        assert_eq!(up.current_path, "/a");

        // 相同路径 enter 不压栈
        let same = PaneNavigationState::root(PaneId::Left, "/a").enter("/a");
        assert!(same.back_stack.is_empty());
    }

    #[test]
    fn replace_resets_stacks() {
        let state = PaneNavigationState::root(PaneId::Left, "/a").enter("/a/b");
        let replaced = state.replace("/x");
        assert_eq!(replaced.current_path, "/x");
        assert!(replaced.back_stack.is_empty());
        assert!(replaced.forward_stack.is_empty());
    }

    #[test]
    fn root_protocol_navigation() {
        let state = PaneNavigationState::root(PaneId::Left, "root:///sdcard");
        assert!(state.can_go_up());
        let up = state.go_up();
        assert_eq!(up.current_path, "root:///");
        assert!(!up.can_go_up());
    }

    #[test]
    fn dual_pane_navigation_update_and_swap() {
        let dual = DualPaneNavigationState::roots("/a", "/b");
        let entered = dual.enter(PaneId::Left, "/a/x");
        assert_eq!(entered.pane(PaneId::Left).current_path, "/a/x");
        assert_eq!(entered.pane(PaneId::Right).current_path, "/b");

        let swapped = entered.swap_panes();
        assert_eq!(swapped.pane(PaneId::Left).current_path, "/b");
        assert_eq!(swapped.pane(PaneId::Right).current_path, "/a/x");
        assert_eq!(swapped.pane(PaneId::Right).pane_id, PaneId::Right);
    }

    #[test]
    fn transfer_target_resolution() {
        let pane_state = DualPaneState::new(true).focus(PaneId::Right);
        let navigation = DualPaneNavigationState::roots("/left", "/right");
        let target = PaneTransferTargetResolver::resolve(&pane_state, &navigation).unwrap();
        assert_eq!(target.source_pane, PaneId::Right);
        assert_eq!(target.target_pane, PaneId::Left);
        assert_eq!(target.source_path, "/right");
        assert_eq!(target.target_path, "/left");
        assert!(target.has_distinct_target_path());
        assert!(target.is_explicit_dual_pane_target);

        let single = DualPaneState::new(false);
        assert!(PaneTransferTargetResolver::resolve(&single, &navigation).is_none());
    }
}
