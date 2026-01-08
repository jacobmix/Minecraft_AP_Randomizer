def _with_tk_root(func):
    """
    Helper to create a properly-focused, modal Tk root for messageboxes.
    """
    try:
        import tkinter as tk

        root = tk.Tk()
        root.withdraw()

        # Focus / z-order fixes
        root.attributes("-topmost", True)
        root.lift()
        root.focus_force()

        try:
            return func(root)
        finally:
            # Release topmost and clean up
            root.attributes("-topmost", False)
            root.destroy()

    except Exception:
        return None


def yes_no(title: str, message: str) -> bool:
    try:
        from tkinter import messagebox

        result = _with_tk_root(
            lambda root: messagebox.askyesno(
                title,
                message,
                parent=root  # critical: ties dialog to focused root
            )
        )

        if result is None:
            raise RuntimeError

        return result

    except Exception:
        print(f"{title}\n{message}")
        return input("Continue? [y/N]: ").lower().startswith("y")


def info(title: str, message: str):
    try:
        from tkinter import messagebox

        result = _with_tk_root(
            lambda root: messagebox.showinfo(
                title,
                message,
                parent=root
            )
        )

        if result is None:
            raise RuntimeError

    except Exception:
        print(f"{title}\n{message}")
