# Public config library

The in-game **Public** tab reads named JSON profiles from this account's repository:

`https://github.com/lunba38-pixel/lunba-client-configs/tree/main/configs`

Anyone can browse and load the published profiles. To publish from the game, create a fine-grained personal access token restricted to the `lunba-client-configs` repository with **Contents: Read and write** permission, then save only the token text at:

`<Minecraft instance>/config/lunba-client/github-token.txt`

The token is read from the local game configuration directory and is not stored in the mod or uploaded to the repository. Never put it inside this project's `configs/` folder or share it with friends. Friends do not need a token to download configs.

To publish, open the in-game Configs screen, select **Local**, enter a name, turn on **Public**, and choose **Save + publish**. In the **Public** tab, choose **Refresh** and click a profile row to load it.
