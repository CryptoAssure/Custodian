# Custodian

[Custodian](https://github.com/CryptoAssure/Custodian) is a toolset designed to make it easier for Nu custodians to manage their automated trading bots and to simplify the effort required to publicly report information about a grant's operations.

v0.1.0-dev

## Implemented Functionality

Docker-based isolated containers for running different Nu automated trading agents ([NuBot](https://bitbucket.org/JordanLeePeershares/nubottrading/overview)); customizable for different exchanges and markets.


## Development Notes

Custodian is under heavy development and is not intended to be used in a production environment at this time. 

**Unless otherwise stated**, the software on this site is provided "as-is," without any express or implied warranty. In no event shall the developers be held liable for any damages arising from the use of the software.

## Future Feature Roadmap

 1. Custodial Operations Management
   1. Web-based application (Node.js) with authentication to administrative tools.
   1. Coordination tool for launching multiple market bots and managing their operations (starting, stopping, scheduled tasks, etc.)
   1. Ability to run the Nu daemon as a stand-alone container, or to connect to a remotely accessible node to report liquidity information to the network.
   1. Email notifications
 1. Reporting
   1. Persistant, consolidated data storage and search functionality for operation and trading logs.
   1. Publicly accessible set of (administrator customizable) reports intended to keep the Nu shareholders and the larger Nu community appraised of a custodian's operations.

## Getting Started

Custodian requires [Docker](http://docker.io). For pre-release versions, the Dockerfile can be located in the `.docker/bot` directory.

After installing Docker, the automated agent's container image will need to be build locally. You can tag it anything you want but for this example I'll use "bot_test" as the tag name. To build an image from the command line:

```
$ cd .docker/bot
$ sudo docker build -t bot_test .
```

You'll see a flow of processes being run (this only needs to be run once, unless you've made changes to the Dockerfile). The text, "Successfully built {image_hash}" will appear if everything worked as expected.

To launch the container, in interactive mode:

```
$ sudo docker run -t -i bot_test /bin/bash
```
> NOTE: A future release of Custodian will include a "start bot daemon on launch" feature, but for now it will need to be manually started, restarted, or stopped for testing from inside of the container.

After running the command you'll be dropped into a bash shell. From there you can manually intereact with the NuBot daemon.

This repository comes loaded with the NuBot binary and associated resource files, but to make it work correctly, you'll need to copy one of the `options-sample-*.txt` configuration files found in `.docker/bot/src/` (depending on the level of customization you want to set). 

```
$ cp options-sample-extended.txt options.txt
```

Only you know the information that you'll need to provide, so for that, you are on your own. The [NuBot documentation](https://bitbucket.org/JordanLeePeershares/nubottrading/overview) is pretty extensive if you need help.

> Tip: The file `options.txt` has been added to `.gitignore` to prevent inadvertent publication of private key information back up to a public place if this repo is forked in the future.

Navigate to the `/data/services/` directory where you can find the service initialization scripts, `start.sh`, `restart.sh`, and `stop.sh`.

```
// To launch the bot
[ root@123abc:/data/services ]$ ./start

// To restart the bot
[ root@123abc:/data/services ]$ ./restart

// To stop the bot
[ root@123abc:/data/services ]$ ./stop
```

Once the bot has been started, two files are created in the `/data/services/` directory: `bot-*.pid` and `operations.log`. 

The file, `operations.log` is just a replication of the bot's main log file accessible at `/data/src/logs/`. You can easily use `tail -f` to confirm that the bot has started up properly.

```
[ root@123abc:/data/services ]$ tail -f operations.log
```

Once you are done with your test, you can exit the shell session with the command `exit`. Once it closes, the test container is destroyed.

That's it, for now. Keep an eye on the repo for upcoming updates!